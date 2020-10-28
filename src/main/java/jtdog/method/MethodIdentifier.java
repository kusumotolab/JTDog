package jtdog.method;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class MethodIdentifier {
    private String classBinaryName;
    private String binaryName;
    private String returnType;
    private ArrayList<String> parameterTypes;

    public MethodIdentifier(IMethodBinding methodBinding) {
        final ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        this.classBinaryName = declaringClass.getBinaryName();
        this.binaryName = classBinaryName + "." + methodBinding.getName();
        this.returnType = methodBinding.getReturnType().getBinaryName();
        this.parameterTypes = new ArrayList<>();

        ITypeBinding[] typeBindings = methodBinding.getParameterTypes();
        if (typeBindings.length != 0) {
            for (ITypeBinding typeBinding : typeBindings) {
                addParameterType(typeBinding.getBinaryName());
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.binaryName != null ? this.binaryName.hashCode() : 0);
        hash = 67 * hash + (this.returnType != null ? this.returnType.hashCode() : 0);
        hash = 67 * hash + (this.parameterTypes != null ? this.parameterTypes.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MethodIdentifier identifier = (MethodIdentifier) obj;
        if (!identifier.getBinaryName().equals(this.binaryName)) {
            return false;
        }
        if (!identifier.getParameterTypes().equals(this.parameterTypes)) {
            return false;
        }
        if (!identifier.getReturnType().equals(this.returnType)) {
            return false;
        }

        return true;
    }

    public String getClassBinaryName() {
        return classBinaryName;
    }

    public void setClassBinaryName(String classBinaryName) {
        this.classBinaryName = classBinaryName;
    }

    public String getBinaryName() {
        return binaryName;
    }

    public void setBinaryName(String binaryName) {
        this.binaryName = binaryName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public ArrayList<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(ArrayList<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public void addParameterType(String parameterType) {
        parameterTypes.add(parameterType);
    }

}
