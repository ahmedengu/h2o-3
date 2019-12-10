package hex.genmodel.tools;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import hex.genmodel.GenModel;
import hex.genmodel.MojoModel;
import hex.genmodel.algos.tree.SharedTreeGraph;
import hex.genmodel.algos.tree.SharedTreeGraphConverter;

import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.*;

import org.jgrapht.io.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Print dot (graphviz) representation of one or more trees in a DRF or GBM model.
 */
public class PrintMojo {
  private GenModel genModel;
  private boolean printRaw = false;
  private int treeToPrint = -1;
  private static int maxLevelsToPrintPerEdge = 10;
  private boolean detail = false;
  private String outputFileName = null;
  private String optionalTitle = null;
  private PrintTreeOptions pTreeOptions;
  private boolean internal;
  private String outputFormat = "dot";
  private static final String tmpOutputFileName = "tmpOutputFileName.gv";

  public static void main(String[] args) {
    // Parse command line arguments
    PrintMojo main = new PrintMojo();
    main.parseArgs(args);

    // Run the main program
    try {
      main.run();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }

    // Success
    System.exit(0);
  }

  private void loadMojo(String modelName) throws IOException {
    genModel = MojoModel.load(modelName);
  }

  private static void usage() {
    System.out.println("Emit a human-consumable graph of a model for use with dot (graphviz).");
    System.out.println("The currently supported model types are DRF, GBM and XGBoost.");
    System.out.println("");
    System.out.println("Usage:  java [...java args...] hex.genmodel.tools.PrintMojo [--tree n] [--levels n] [--title sss] [-o outputFileName] [--direct imageOutputFileName]");
    System.out.println("");
    System.out.println("    --tree          Tree number to print.");
    System.out.println("                    [default all]");
    System.out.println("");
    System.out.println("    --levels        Number of levels per edge to print.");
    System.out.println("                    [default " + maxLevelsToPrintPerEdge + "]");
    System.out.println("");
    System.out.println("    --title         (Optional) Force title of tree graph.");
    System.out.println("");
    System.out.println("    --detail        Specify to print additional detailed information like node numbers.");
    System.out.println("");
    System.out.println("    --input | -i    Input mojo file.");
    System.out.println("");
    System.out.println("    --output | -o   Output dot/png filename.");
    System.out.println("                    [default stdout]");
    System.out.println("    --decimalplaces | -d    Set decimal places of all numerical values.");
    System.out.println("");
    System.out.println("    --fontsize | -f    Set font sizes of strings.");
    System.out.println("");
    System.out.println("    --internal    Internal H2O representation of the decision tree (splits etc.) is used for generating the GRAPHVIZ format.");
    System.out.println("");
    System.out.println("    --format    Specify output format of the decision tree representation (dot or png) ");
    System.out.println("                    [default dot]");
    System.out.println("");
    System.out.println("");
    System.out.println("Example:");
    System.out.println("");
    System.out.println("    (brew install graphviz)");
    System.out.println("    java -cp h2o.jar hex.genmodel.tools.PrintMojo --tree 0 -i model_mojo.zip -o model.gv -f 20 -d 3");
    System.out.println("    dot -Tpng model.gv -o model.png");
    System.out.println("    open model.png");
    System.out.println();
    System.exit(1);
  }

  private void parseArgs(String[] args) {
    int nPlaces = -1;
    int fontSize = 14; // default size is 14
    boolean setDecimalPlaces = false;
    try {
      for (int i = 0; i < args.length; i++) {
        String s = args[i];
        switch (s) {
          case "--tree":
            i++;
            if (i >= args.length) usage();
            s = args[i];
            try {
              treeToPrint = Integer.parseInt(s);
            } catch (Exception e) {
              System.out.println("ERROR: invalid --tree argument (" + s + ")");
              System.exit(1);
            }
            break;

          case "--levels":
            i++;
            if (i >= args.length) usage();
            s = args[i];
            try {
              maxLevelsToPrintPerEdge = Integer.parseInt(s);
            } catch (Exception e) {
              System.out.println("ERROR: invalid --levels argument (" + s + ")");
              System.exit(1);
            }
            break;

          case "--title":
            i++;
            if (i >= args.length) usage();
            optionalTitle = args[i];
            break;

          case "--detail":
            detail = true;
            break;

          case "--input":
          case "-i":
            i++;
            if (i >= args.length) usage();
            s = args[i];
            loadMojo(s);
            break;

          case "--fontsize":
          case "-f":
            i++;
            if (i >= args.length) usage();
            s = args[i];
            fontSize = Integer.parseInt(s);
            break;

          case "--decimalplaces":
          case "-d":
            i++;
            if (i >= args.length) usage();
            setDecimalPlaces=true;
            s = args[i];
            nPlaces = Integer.parseInt(s);
            break;

          case "--format":
            i++;
            if (i >= args.length) usage();
            outputFormat = args[i];
            if(!(outputFormat.equalsIgnoreCase("png") || outputFormat.equalsIgnoreCase("dot"))) {
              System.out.println("ERROR: Unsupported format");
              usage();
            }
            break;
            
          case "--raw":
            printRaw = true;
            break;
          case "--internal":
            internal = true;
            break;

          case "-o":
          case "--output":
            i++;
            if (i >= args.length) usage();
            outputFileName = args[i];
            break;

          default:
            System.out.println("ERROR: Unknown command line argument: " + s);
            usage();
            break;
        }
      }
      pTreeOptions = new PrintTreeOptions(setDecimalPlaces, nPlaces, fontSize, internal);
    } catch (Exception e) {
      e.printStackTrace();
      usage();
    }
  }

  private void validateArgs() {
    if (genModel == null) {
      System.out.println("ERROR: Must specify -i");
      usage();
    }
    if (outputFileName != null && 
            (("png".equalsIgnoreCase(outputFormat) && !outputFileName.toUpperCase().endsWith("PNG")) || 
            ("dot".equalsIgnoreCase(outputFormat) && !(outputFileName.toUpperCase().endsWith("DOT") || outputFileName.toUpperCase().endsWith("GV"))))) {
      System.out.println("ERROR: Output file name \"" + outputFileName + "\"  has invalid file extension. Fix extension or use different output format than \"" + outputFormat + "\".");
      usage();
    }
  }

  private void run() throws Exception {
    validateArgs();
    if ("png".equalsIgnoreCase(outputFormat)) {
      handlePrintingDotToTmpFile();
    } else if (outputFileName != null) {
      handlePrintingDotToOutputFile();
    } else {
      HandlePrintingDotToConsole();
    }
  }
  
  private void handlePrintingDotToOutputFile() throws IOException, ImportException {
    Path dotSourceFilePath = Paths.get(outputFileName);
    try (FileOutputStream fos = new FileOutputStream(dotSourceFilePath.toFile()); PrintStream os = new PrintStream(fos)) {
      generateDotFromSource(os, dotSourceFilePath);
    }
  }

  private void handlePrintingDotToTmpFile() throws IOException, ImportException {
    Path dotSourceFilePath = Files.createTempFile("", tmpOutputFileName);
    try (FileOutputStream fos = new FileOutputStream(dotSourceFilePath.toFile()); PrintStream os = new PrintStream(fos)) {
      generateDotFromSource(os, dotSourceFilePath);
    }
  }

  private void HandlePrintingDotToConsole() throws IOException, ImportException {
    generateDotFromSource(System.out, null);
  }

  private void generateDotFromSource(PrintStream os, Path dotSourceFilePath) throws IOException, ImportException {
    if (!(genModel instanceof SharedTreeGraphConverter)) {
      System.out.println("ERROR: Unknown MOJO type");
      System.exit(1);
    }
    SharedTreeGraphConverter treeBackedModel = (SharedTreeGraphConverter) genModel;
    final SharedTreeGraph g = treeBackedModel.convert(treeToPrint, null);
    if (printRaw) {
      g.print();
    }
    g.printDot(os, maxLevelsToPrintPerEdge, detail, optionalTitle, pTreeOptions);
    if (outputFormat.toUpperCase().equals("PNG")) {
      generateOutputPng(dotSourceFilePath);
    }
  }
  
  private void generateOutputPng(Path dotSourceFilePath) throws ImportException, IOException {
    LabeledVertexProvider vertexProvider = new LabeledVertexProvider();
    LabeledEdgesProvider edgesProvider = new LabeledEdgesProvider();
    ComponentUpdater componentUpdater = new ComponentUpdater();
    DOTImporter<String, LabeledEdge> importer = new DOTImporter<>(vertexProvider, edgesProvider, componentUpdater);
    DirectedMultigraph<String, LabeledEdge> result = new DirectedMultigraph<>(LabeledEdge.class);
    try ( FileInputStream is = new FileInputStream(dotSourceFilePath.toFile())) {
      importer.importGraph(result, new InputStreamReader(is));
      JGraphXAdapter<String, LabeledEdge> graphAdapter = new JGraphXAdapter<String, LabeledEdge>(result);
      mxIGraphLayout layout = new mxCompactTreeLayout(graphAdapter, false);
      layout.execute(graphAdapter.getDefaultParent());
      BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);
      if (outputFileName != null) {
        ImageIO.write(image, "PNG", new File(outputFileName));
      } else {
        ImageIO.write(image, "PNG", System.out);
      }
    }
  }

  public class PrintTreeOptions {
    public boolean _setDecimalPlace = false;
    public int _nPlaces = -1;
    public int _fontSize = 14;  // default
    public boolean _internal;

    public PrintTreeOptions(boolean setdecimalplaces, int nplaces, int fontsize, boolean internal) {
      _setDecimalPlace = setdecimalplaces;
      _nPlaces = _setDecimalPlace?nplaces:_nPlaces;
      _fontSize = fontsize;
      _internal = internal;
    }

    public float roundNPlace(float value) {
      if (_nPlaces < 0)
        return value;
      double sc = Math.pow(10, _nPlaces);
      return (float) (Math.round(value*sc)/sc);
    }
  }

  private class LabeledVertexProvider implements VertexProvider<String> {
    @Override
    public String buildVertex(String id, Map<String, Attribute> attributes) {
      return attributes.get("label").toString();
    }
  }
  
  private class LabeledEdgesProvider implements EdgeProvider<String,LabeledEdge>{

    @Override
    public LabeledEdge buildEdge(String f, String t, String l, Map<String, Attribute> attrs) {
      return new LabeledEdge(l);
    }
  }
  private class ComponentUpdater implements org.jgrapht.io.ComponentUpdater<String>{
      @Override
    public void update(String v, Map<String, Attribute> attrs) {
    }
  }
  
  private class LabeledEdge extends DefaultEdge {
    private String label;

    /**
     * Constructs a relationship edge
     *
     * @param label the label of the new edge.
     *
     */
    public LabeledEdge(String label)
    {
      this.label = label;
    }

    /**
     * Gets the label associated with this edge.
     *
     * @return edge label
     */
    public String getLabel()
    {
      return label;
    }

    @Override
    public String toString()
    {
      return label;
    }
  }

}
