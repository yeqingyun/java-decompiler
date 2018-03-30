package com.github.zxh.classpy.classfile;

import com.github.zxh.classpy.classfile.attribute.AttributeInfo;
import com.github.zxh.classpy.classfile.bytecode.Instruction;
import com.github.zxh.classpy.classfile.constant.ConstantClassInfo;
import com.github.zxh.classpy.classfile.constant.ConstantNameAndTypeInfo;
import com.github.zxh.classpy.classfile.constant.ConstantPool;
import com.github.zxh.classpy.classfile.datatype.Table;
import com.github.zxh.classpy.classfile.datatype.U2;
import com.github.zxh.classpy.classfile.datatype.U2AccessFlags;
import com.github.zxh.classpy.classfile.datatype.U2CpIndex;
import com.github.zxh.classpy.classfile.jvm.AccessFlagType;
import com.github.zxh.classpy.classfile.jvm.AccessFlags;
import com.github.zxh.classpy.common.FileComponent;

import java.util.*;

/*
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
*/
public class ClassFile extends ClassFileComponent {

    public static final Map<Character, String> paramMap = new HashMap<>();

    static {
        paramMap.put('B', "byte");
        paramMap.put('C', "char");
        paramMap.put('D', "double");
        paramMap.put('F', "float");
        paramMap.put('I', "int");
        paramMap.put('J', "long");
        paramMap.put('S', "short");
        paramMap.put('Z', "boolean");
    }

    {
        U2 cpCount = new U2();

        u4hex("magic");
        u2("minor_version");
        u2("major_version");
        add("constant_pool_count", cpCount);
        add("constant_pool", new ConstantPool(cpCount));
        u2af("access_flags", AccessFlagType.AF_CLASS);
        u2cp("this_class");
        u2cp("super_class");
        u2("interfaces_count");
        table("interfaces", U2CpIndex.class);
        u2("fields_count");
        table("fields", FieldInfo.class);
        u2("methods_count");
        table("methods", MethodInfo.class);
        u2("attributes_count");
        table("attributes", AttributeInfo.class);
    }

    public ConstantPool getConstantPool() {
        return (ConstantPool) super.get("constant_pool");
    }

    public String generatePackage() {
        try {
            String thisClassDesc = super.get("this_class").getDesc();
            if (thisClassDesc.lastIndexOf("/") < 0) {
                return null;
            }
            String pg = thisClassDesc.replaceAll("/", ".");
            return "package " + pg.substring(pg.indexOf(">") + 1, pg.lastIndexOf(".")) + ";\n";
        } catch (Exception e) {
            return null;
        }
    }

    public String generateImport() {
        StringBuilder importCode = new StringBuilder();
        String thisClassDesc = super.get("this_class").getDesc();
        List<FileComponent> components = this.getConstantPool().getComponents();
        for (FileComponent component : components) {
            if (component instanceof ConstantClassInfo) {
                if (!thisClassDesc.contains(component.getDesc()) && !component.getDesc().contains("java/lang/") && !component.getDesc().equals("java/io/PrintStream")) {
                    importCode.append("import ").append(component.getDesc().replaceAll("/", ".")).append(";").append("\n");
                }
            }
        }

        return importCode.toString();
    }


    public String generateJava() {
        return new StringBuilder().append(generatePackage()).append(generateImport()).append(generateClassHead()).append("\r\n").append(generateMethod()).append("}").toString();
    }

    public String generateClassHead() {
        StringBuilder javaCode = new StringBuilder();

        U2AccessFlags af = (U2AccessFlags) super.get("access_flags");
        String afDesc = af.getDesc();
        if (afDesc.contains(AccessFlags.ACC_PUBLIC.name())) {
            javaCode.append("public ");
        } else if (afDesc.contains(AccessFlags.ACC_PRIVATE.name())) {
            javaCode.append("private ");
        } else {
            javaCode.append("protected ");
        }

        if (afDesc.contains(AccessFlags.ACC_ABSTRACT.name())) {
            javaCode.append("abstract");
        }

        if (afDesc.contains(AccessFlags.ACC_FINAL.name())) {
            javaCode.append("final ");
        }


        if (afDesc.contains(AccessFlags.ACC_INTERFACE.name())) {
            javaCode.append("interface ");
        } else {
            javaCode.append("class ");
        }
        String thisClassDesc = super.get("this_class").getDesc();
        javaCode.append(thisClassDesc.substring(thisClassDesc.lastIndexOf("/") + 1));

        String superClass = get("super_class").getDesc();
        if (!superClass.contains("java/lang/Object")) {
            javaCode.append(" extends ").append(superClass.substring(superClass.lastIndexOf("/") + 1));
        }

        if (Integer.parseInt(super.get("interfaces_count").getDesc()) > 0) {
            javaCode.append(" implements ");
        }

        Table interfaceTable = (Table) super.get("interfaces");
        for (int i = 0; i < interfaceTable.getComponents().size(); i++) {
            String interfaceName = interfaceTable.getComponents().get(i).getDesc();
            javaCode.append(interfaceName.substring(interfaceName.lastIndexOf("/") + 1));


            if (i != interfaceTable.getComponents().size() - 1) {
                javaCode.append(",");
            }
        }
        javaCode.append(" {");
        return javaCode.toString();
    }

    public String generateMethod() {
        StringBuilder methodString = new StringBuilder();
        List<FileComponent> components = this.get("methods").getComponents();
        for (FileComponent component : components) {
            MethodInfo method = (MethodInfo) component;

            methodString.append("\t");
            generateMethodHead(methodString, method);
            methodString.append("\n");
            generateMethodBody(methodString, method);

        }
        return methodString.toString();
    }

    public String generateFileds() {
        return null;
    }

    private void generateMethodBody(StringBuilder methodString, MethodInfo method) {
        List<FileComponent> info = method.getComponents();
        if ("<init>".equals(method.getDesc())) {
            return;
        }
        Table attributes = (Table) info.get(4);
        List<String> paramNames = getParamNames(attributes.getComponents());
        for (FileComponent attribute : attributes.getComponents()) {
            if (attribute.getName().contains("Code")) {
                List<FileComponent> codeList = attribute.getComponents().get(5).getComponents();
                //List localVariableTable = attribute.getComponents().get(9).getComponents().get(1).getComponents().get(3).getComponents();

                Stack<Object> opStack = new Stack<>();
                paramNames.add(0, "this");
                for (FileComponent code : codeList) {
                    Instruction icode = ((Instruction) code);
                    String opCode = icode.getOpcode().name();

                    if ("aloda_0".equals(opCode)) {
                        opStack.push(paramNames.get(0));
                    } else if ("invokespecial".equals(opCode)) {
                        int methodRefIndex = ((U2CpIndex) icode.getComponents().get(1)).getValue();

                        FileComponent methodRefInfo = getConstantPool().getComponents().get(methodRefIndex - 1);
                        ConstantNameAndTypeInfo nameAndTypeInfo = (ConstantNameAndTypeInfo) getConstantPool().getComponents().get(((U2) methodRefInfo.getComponents().get(2)).getValue() - 1);

                        String typeDescriptor = getConstantPool().getComponents().get(((U2) nameAndTypeInfo.getComponents().get(2)).getValue() - 1).getDesc();
                        int methodParameterSize = getParamSize(typeDescriptor);
                        Object targetClassName;
                        Object parameterNames[] = new Object[methodParameterSize];

                        if (methodParameterSize > 0) {
                            for (int x = 0; x < methodParameterSize; x++) {
                                parameterNames[methodParameterSize - x - 1] = opStack.pop();
                            }
                        }
                        targetClassName = opStack.pop();

                        StringBuilder line = new StringBuilder();
                        if (method.getDesc().equals("<init>") && targetClassName.equals("this")) {
                            line.append("super");
                        } else {
                            line.append("new ").append(targetClassName);
                        }

                        line.append("(");
                        for (int x = 0; x < methodParameterSize; x++) {
                            line.append(parameterNames[x]);
                            if ((x != methodParameterSize - 1)) {
                                line.append(",");
                            }
                        }
                        line.append(")");

                        opStack.push(line.toString());
                    } else if ("_new".equals(opCode)) {
                        String classDesc = icode.getComponents().get(1).getDesc();
                        opStack.push(classDesc.substring(classDesc.lastIndexOf("/") + 1));
                    } else if ("dup".equals(opCode)) {
                        if (opStack.size() > 0) {
                            //&& !"invokespecial".equals(((Instruction) codeList.get(i + 1)).getOpcode().name())
//                            Object top = opStack.pop();
//                            opStack.push(top);
//                            opStack.push(top);
                        }
                    } else if ("invokevirtual".equals(opCode)) {
                        int methodRefIndex = ((U2CpIndex) icode.getComponents().get(1)).getValue();

                        FileComponent methodRefInfo = getConstantPool().getComponents().get(methodRefIndex - 1);
                        ConstantNameAndTypeInfo nameAndTypeInfo = (ConstantNameAndTypeInfo) getConstantPool().getComponents().get(((U2) methodRefInfo.getComponents().get(2)).getValue() - 1);

                        String typeDescriptor = getConstantPool().getComponents().get(((U2) nameAndTypeInfo.getComponents().get(2)).getValue() - 1).getDesc();
                        int methodParameterSize = getParamSize(typeDescriptor);
                        Object targetClassName = "";
                        Object parameterNames[] = new Object[methodParameterSize];

                        if (methodParameterSize > 0) {
                            for (int x = 0; x < methodParameterSize; x++) {
                                parameterNames[methodParameterSize - x - 1] = opStack.pop();
                            }
                        }
                        if (opStack.size() > 0)
                            targetClassName = opStack.pop();

                        StringBuilder line = new StringBuilder();
                        String methodDesc = icode.getComponents().get(1).getDesc();
                        line.append(targetClassName).append(".").append(methodDesc.substring(methodDesc.lastIndexOf(".") + 1)).append("(");
                        for (int x = 0; x < methodParameterSize; x++) {
                            line.append(parameterNames[x]);
                            if ((x != methodParameterSize - 1)) {
                                line.append(",");
                            }
                        }
                        line.append(")");

                        opStack.push(line.toString());
                    } else if ("ldc".equals(opCode)) {
                        opStack.push("\"" + icode.getComponents().get(1).getDesc().substring(icode.getComponents().get(1).getDesc().indexOf(">") + 1) + "\"");
                    } else if (opCode.contains("iload")) {
                        String index = opCode.substring(opCode.indexOf("_") + 1);
                        opStack.push(paramNames.get(Integer.parseInt(index)));
                    } else if ("iadd".equals(opCode)) {
                        Object p1 = opStack.pop();
                        Object p2 = opStack.pop();
                        opStack.push(p2 + "+" + p1);
                    } else if ("idiv".equals(opCode)) {
                        Object p1 = opStack.pop();
                        Object p2 = opStack.pop();
                        opStack.push(p2 + "/" + p1);
                    } else if ("imul".equals(opCode)) {
                        Object p1 = opStack.pop();
                        Object p2 = opStack.pop();
                        opStack.push(p2 + "*" + p1);
                    } else if ("isub".equals(opCode)) {
                        Object p1 = opStack.pop();
                        Object p2 = opStack.pop();
                        opStack.push(p2 + "-" + p1);
                    } else if ("irem".equals(opCode)) {
                        Object p1 = opStack.pop();
                        Object p2 = opStack.pop();
                        opStack.push(p2 + "%" + p1);
                    } else if ("iand".equals(opCode)) {
                        Object p1 = opStack.pop();
                        Object p2 = opStack.pop();
                        opStack.push(p2 + "&" + p1);
                    } else if (opCode.contains("iconst")) {
                        String cons = opCode.substring(opCode.indexOf("_") + 1);
                        opStack.push(cons);
                    } else if (opCode.contains("astore")) {
                        String obj = opStack.pop().toString();
                        String className = opStack.pop().toString();
                        String cons = opCode.substring(opCode.indexOf("_") + 1);
                        paramNames.set(Integer.parseInt(cons), "localVar1");
                        opStack.push(className + " localVar1=" + obj);
                    } else if (opCode.contains("aload")) {
                        int index = Integer.parseInt(opCode.substring(opCode.indexOf("_") + 1));
                        opStack.push(paramNames.get(index));
                    } else if ("ireturn".equals(opCode) || "_return".equals(opCode) || "areturn".equals(opCode)) {
                        if (method.getComponents().get(2).getDesc().toCharArray()[method.getComponents().get(2).getDesc().indexOf(")") + 1] != 'V') {
                            Object result = opStack.pop();
                            opStack.push("return " + result);
                        }
                    } else if ("getstatic".equals(opCode) || "pop".equals(opCode)) {
                        //TODO
                        String staticDesc = icode.getComponents().get(1).getDesc();
                        opStack.push(staticDesc.substring(staticDesc.lastIndexOf("/") + 1));
                    } else {
                        throw new RuntimeException("undefined instruct: " + opCode);
                    }
                }

                for (Object s : opStack) {
                    methodString.append("\t\t").append(s).append(";\r\n");
                }
                methodString.append("\t}\r\n");
            }
        }
    }

    private int getParamSize(String typeDesc) {
        String pms = typeDesc.substring(typeDesc.indexOf("(") + 1, typeDesc.lastIndexOf(")"));
        int i = 0;
        char[] chars = pms.toCharArray();

        for (int x = 0; x < chars.length; x++) {
            char cr = chars[x];
            if (cr == '[') {
                int j = x + 1;
                while (chars[j] == '[') {
                    x++;
                }
                i++;
            } else if (cr == 'L') {
                x = pms.indexOf(";", x);
                i++;
            } else {
                i++;
            }
        }

        return i;
    }


    private void generateMethodHead(StringBuilder methodString, MethodInfo method) {
        if (!"<init>".equals(method.getDesc())) {
            List<FileComponent> info = method.getComponents();
            String access = info.get(0).getDesc();
            if (access.contains(AccessFlags.ACC_PUBLIC.name())) {
                methodString.append("public ");
            } else if (access.contains(AccessFlags.ACC_PRIVATE.name())) {
                methodString.append("private ");
            } else if (access.contains(AccessFlags.ACC_PROTECTED.name())) {
                methodString.append("protected ");
            }

            if (access.contains(AccessFlags.ACC_ABSTRACT.name())) {
                methodString.append("abstract ");
            }

            if (access.contains(AccessFlags.ACC_STATIC.name())) {
                methodString.append("static ");
            }

            if (access.contains(AccessFlags.ACC_FINAL.name())) {
                methodString.append("final ");
            }

            String methodDsec = info.get(2).getDesc();

            String returnType = methodDsec.substring(methodDsec.lastIndexOf(")") + 1);

            char[] returnChars = returnType.toCharArray();
            if (returnChars.length > 1 && returnChars[0] == '[') {
                Node bracketNode = generateBracket(returnType);
                char cs = returnChars[bracketNode.index];
                if (cs == 'L') {
                    generateLparam(methodString, returnType, bracketNode.index);
                } else {
                    methodString.append(paramMap.get(cs));
                }
                methodString.append(bracketNode.name);
            } else {
                if (paramMap.containsKey(returnChars[0])) {
                    methodString.append(paramMap.get(returnChars[0]));
                } else if (returnChars[0] == 'V') {
                    methodString.append("void");
                } else {
                    generateLparam(methodString, returnType, 0);
                }
            }

            methodString.append(" ");
            String params = methodDsec.substring(methodDsec.indexOf("(") + 1, methodDsec.lastIndexOf(")"));
            methodString.append(method.getDesc());
            methodString.append("(");
            char[] parray = params.toCharArray();
            int i = 0;

            List<FileComponent> attributes = info.get(4).getComponents();
            List<String> paramNames = getParamNames(attributes);


            for (int j = 0; j < parray.length; j++) {
                char p = parray[j];
                if (paramMap.containsKey(p)) {
                    methodString.append(paramMap.get(p)).append(" ").append(paramNames.get(i++)).append(",");
                } else if (p == '[') {

                    Node bracketNode = generateBracket(params.substring(j));
                    j += bracketNode.index;
                    p = parray[j];
                    if (parray[j] == 'L') {
                        j += generateLparam(methodString, params, j);
                    } else {
                        methodString.append(paramMap.get(p));
                    }
                    methodString.append(bracketNode.name).append(" ").append(paramNames.get(i++)).append(",");
                } else {
                    generateLparam(methodString, params, j);
                    methodString.append(" ").append(paramNames.get(i++)).append(",");
                }
            }
            if (methodString.charAt(methodString.length() - 1) == ',')
                methodString.deleteCharAt(methodString.length() - 1);

            methodString.append(")");

            Table sattributes = (Table) info.get(4);

            for (FileComponent attribute : sattributes.getComponents()) {
                if (attribute.getName().contains("Exceptions")) {
                    methodString.append(" throws ");
                    Table exceptionTable = (Table) attribute.getComponents().get(3);
                    for (FileComponent exception : exceptionTable.getComponents()) {
                        String exceptionClass = exception.getDesc();
                        methodString.append(exceptionClass.substring(exceptionClass.lastIndexOf("/") + 1)).append(",");
                    }
                }
            }

            if (methodString.charAt(methodString.length() - 1) == ',')
                methodString.deleteCharAt(methodString.length() - 1);

            methodString.append(" { ");

        }
    }

    private List<String> getParamNames(List<FileComponent> attributes) {
        List<String> paramNames = new ArrayList<>();
        for (FileComponent attribute : attributes) {
            if (attribute.getName().contains("Code")) {
                List<FileComponent> variableTable = attribute.getComponents().get(9).getComponents().get(1).getComponents().get(3).getComponents();
                for (FileComponent fileComponent : variableTable) {
                    String name = fileComponent.getDesc().substring(0, fileComponent.getDesc().indexOf("("));
                    if (!"this".equals(name))
                        paramNames.add(name);
                }
            }
        }
        return paramNames;
    }

    private int generateLparam(StringBuilder methodString, String params, int j) {
        String tmp = params.substring(j + 1, params.indexOf(";", j));
        String className = tmp.substring(tmp.lastIndexOf("/") + 1);
        methodString.append(className);
        return tmp.length() + 1;
    }

    private Node generateBracket(String left) {
        char[] chars = left.toCharArray();
        StringBuilder brackets = new StringBuilder();
        int i = 0;
        for (; i < chars.length; i++) {
            if (chars[i] == '[') {
                brackets.append("[]");
            } else {
                break;
            }
        }

        return new Node(brackets.toString(), i);
    }


    public class Node {
        String name;
        int index;

        public Node(String name, int index) {
            this.name = name;
            this.index = index;
        }
    }

}
