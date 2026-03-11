
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import in.ac.iiitb.plproject.atc.generated.Helper;

public class GeneratedTests {

@Test
void test_0() {
    assertEquals("Toddler", Helper.ageCategoryPrint(0));
}

@Test
void test_1() {
    assertEquals("Kid", Helper.ageCategoryPrint(6));
}

@Test
void test_2() {
    assertEquals("Teenager", Helper.ageCategoryPrint(13));
}

@Test
void test_3() {
    assertEquals("Adult", Helper.ageCategoryPrint(21));
}

@Test
void test_4() {
    assertEquals("Midlife", Helper.ageCategoryPrint(46));
}

@Test
void test_5() {
    assertEquals("Senior citizen", Helper.ageCategoryPrint(66));
}

}