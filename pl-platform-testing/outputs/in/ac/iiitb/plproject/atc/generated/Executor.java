package in.ac.iiitb.plproject.atc.generated;
public class Executor {

    public static void main(String[] args) {

        int age = Integer.parseInt(args[0]);

        String result = Helper.ageCategoryPrint(age);

        System.out.println(result);
    }
}