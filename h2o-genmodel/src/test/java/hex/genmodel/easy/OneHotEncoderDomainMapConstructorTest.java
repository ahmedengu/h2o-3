package hex.genmodel.easy;

import hex.genmodel.CategoricalEncoding;
import hex.genmodel.GenModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class OneHotEncoderDomainMapConstructorTest {

  @Mock
  private GenModel mockModel;

  @Test
  public void create() {
    Map<String, Integer> columnNameToIndex = new HashMap<>();
    columnNameToIndex.put("col1", 0);
    columnNameToIndex.put("col2", 3);
    columnNameToIndex.put("col3", 4);
    
    when(mockModel.getOrigNames()).thenReturn(new String[]{"col1", "col2", "col3", "response"});
    when(mockModel.getOrigDomainValues()).thenReturn(new String[][]{
            new String[]{"a", "b", "c"},
            null,
            new String[]{"x"},
            new String[]{"any", "thing"}
    });
    when(mockModel.getOrigNumCols()).thenReturn(3);
    
    Map<Integer, CategoricalEncoder> dm = CategoricalEncoding.OneHotExplicit.createCategoricalEncoders(mockModel, columnNameToIndex);
    assertEquals(new HashSet<>(Arrays.asList(0, 2)), dm.keySet());
    for (CategoricalEncoder ce : dm.values()) {
      assertTrue(ce instanceof OneHotEncoder);
    }
  }
}
