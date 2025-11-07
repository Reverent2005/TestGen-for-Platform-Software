package in.ac.iiitb.plproject.parser.ast;

/**
 * Represents a variable with a name and type.
 */
public class Variable {
    private String name;
    private String typeName; // Using String for now, can be converted to Type when needed

    public Variable(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    public Variable(String name, Object type) {
        // Overload for compatibility - accepts Type from ast package
        this.name = name;
        this.typeName = type.toString(); // Convert Type to string representation
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }
    
    @Override
    public String toString() {
        return name + ": " + typeName;
    }
}
