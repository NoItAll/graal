/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.truffle.espresso;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.tools.JavaFileObject;

public class SubstitutionProcessor extends AbstractProcessor {
    private TypeElement espressoSubstitutions;
    private TypeElement substitutions;
    private ExecutableElement hasReceiverElement;

    private static final String SUBSTITUTION_PACKAGE = "com.oracle.truffle.espresso.substitutions";

    private static final String ESPRESSO_SUBSTITUTIONS = SUBSTITUTION_PACKAGE + "EspressoSubstitutions";
    private static final String METHOD_SUBSTITUTION = SUBSTITUTION_PACKAGE + "Substitution";

    private static final String INSTANCE_NAME = "theInstance";
    private static final String GETTER = "getInstance";

    private static final String getClassName(String className, String methodName) {
        return className + "_" + methodName;
    }

    private static final String getQualifiedClassName(String className, String methodName) {
        return SUBSTITUTION_PACKAGE + "." + getClassName(className, methodName);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.espressoSubstitutions = processingEnvironment.getElementUtils().getTypeElement(ESPRESSO_SUBSTITUTIONS);
        this.substitutions = processingEnvironment.getElementUtils().getTypeElement(METHOD_SUBSTITUTION);
        for (Element e : substitutions.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD && e.getSimpleName().contentEquals("hasReceiver")) {
                this.hasReceiverElement = (ExecutableElement) e;
            }
        }

    }

    private static final String COPYRIGHT = "/* Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.\n" +
                    " * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.\n" +
                    " *\n" +
                    " * This code is free software; you can redistribute it and/or modify it\n" +
                    " * under the terms of the GNU General Public License version 2 only, as\n" +
 * published by the Free Software Foundation.
                    " *\n" +
                    " * This code is distributed in the hope that it will be useful, but WITHOUT\n" +
                    " * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n" +
                    " * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n" +
                    " * version 2 for more details (a copy is included in the LICENSE file that\n" +
                    " * accompanied this code).\n" +
                    " *\n" +
                    " * You should have received a copy of the GNU General Public License version\n" +
                    " * 2 along with this work; if not, write to the Free Software Foundation,\n" +
                    " * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.\n" +
                    " *\n" +
                    " * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA\n" +
                    " * or visit www.oracle.com if you need additional information or have any\n" +
                    " * questions.\n" +
                    " */\n\n";
    private static final String GENERATED_BY = "// Generated by: ";
    private static final String PACKAGE_INFO = "package com.oracle.truffle.espresso.substitutions;\n" +
                    "\n" +
                    "import com.oracle.truffle.espresso.runtime.StaticObject;\n" +
                    "import com.oracle.truffle.espresso.substitutions.Substitutor;\n";

    private static final String PRIVATE_STATIC_FINAL = "private static final";
    private static final String PUBLIC_STATIC_FINAL = "public static final";
    private static final String PUBLIC_FINAL_OBJECT = "public final Object ";
    private static final String PUBLIC_FINAL_CLASS = "\n" + "public final class ";
    private static final String SUBSTITUTOR = "Substitutor";
    private static final String EXTENSION = " extends " + SUBSTITUTOR + " {\n";
    private static final String OVERRIDE = "@Override";
    private static final String ARGS_NAME = "args";
    private static final String INVOKE = "invoke(Object[] " + ARGS_NAME + ") {\n";
    private static final String ARG_NAME = "arg";
    private static final String TAB_1 = "    ";
    private static final String TAB_2 = TAB_1 + TAB_1;

    private static String castTo(String obj, String clazz) {
        return "(" + clazz + ") " + obj;
    }

    private static String extractArg(int index, String clazz, String tabulation) {
        return tabulation + clazz + " " + ARG_NAME + index + " = " + castTo(ARGS_NAME + "[" + index + "]", clazz) + ";\n";
    }

    private static String extractInvocation(String className, String methodName, int nParameters) {
        StringBuilder str = new StringBuilder();
        str.append(className).append(".").append(methodName).append("(");
        boolean notFirst = false;
        for (int i = 0; i < nParameters; i++) {
            if (notFirst) {
                str.append(", ");
            } else {
                notFirst = true;
            }
            str.append(ARG_NAME).append(i);
        }
        str.append(");\n");
        return str.toString();
    }

    private static String generateConstructor(String substitutorName) {
        StringBuilder str = new StringBuilder();
        str.append(TAB_1).append("private ").append(substitutorName).append("() {\n").append(TAB_1).append("}\n");
        return str.toString();
    }

    private static String generateInstance(String substitutorName) {
        StringBuilder str = new StringBuilder();
        str.append(TAB_1).append(PRIVATE_STATIC_FINAL).append(" ").append(SUBSTITUTOR).append(" ").append(INSTANCE_NAME);
        str.append(" = new ").append(substitutorName).append("();\n");
        return str.toString();
    }

    private static String generateGetter() {
        StringBuilder str = new StringBuilder();
        str.append(TAB_1).append(PUBLIC_STATIC_FINAL).append(" ").append(SUBSTITUTOR).append(" ").append(GETTER).append("() {\n");
        str.append(TAB_2).append("return ").append(INSTANCE_NAME).append(";\n");
        str.append(TAB_1).append("}\n");
        return str.toString();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        System.err.println("launched");
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        System.err.println("launched");
        Set<String> annotations = new HashSet<>();
        annotations.add(ESPRESSO_SUBSTITUTIONS);
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.err.println("launched");
        if (!roundEnv.processingOver()) {
            processImpl(roundEnv);
        }
        return false;
    }

    private void processImpl(RoundEnvironment env) {
        for (Element e : env.getElementsAnnotatedWith(espressoSubstitutions)) {
            processElement(e);
        }
    }

    private void processElement(Element e) {
        assert e.getKind() == ElementKind.CLASS;
        TypeElement typeElement = (TypeElement) e;
        String className = typeElement.getQualifiedName().toString();
        for (Element inner : e.getEnclosedElements()) {
            List<? extends AnnotationMirror> annotations = inner.getAnnotationMirrors();
            AnnotationMirror annotation = null;
            for (AnnotationMirror mirror : annotations) {
                if (mirror.getAnnotationType().asElement().equals(substitutions)) {
                    annotation = mirror;
                    break;
                }
            }
            if (annotation != null) {
                assert inner.getKind() == ElementKind.METHOD;
                String methodName = inner.getSimpleName().toString();
                AnnotationValue annotationValue = processingEnv.getElementUtils().getElementValuesWithDefaults(annotation).get(hasReceiverElement);
                if (annotationValue != null) {
                    boolean hasReceiver = (boolean) annotationValue.getValue();
                    String classFile = spawnSubstitutor(className, methodName, getParameterTypes((ExecutableElement) inner), hasReceiver,
                                    ((ExecutableElement) inner).getReturnType().toString().equals("void"));
                    try {
                        JavaFileObject file = processingEnv.getFiler().createSourceFile(getQualifiedClassName(className, methodName), inner);
                        Writer wr = file.openWriter();
                        wr.write(classFile);
                        wr.close();
                        wr.flush();
                    } catch (IOException ex) {
                        /* nop */
                    }
                }
            }
        }
    }

    private static List<String> getParameterTypes(ExecutableElement inner) {
        ArrayList<String> parameterTypeNames = new ArrayList<>();
        for (TypeParameterElement parameter : inner.getTypeParameters()) {
            parameterTypeNames.add(parameter.getGenericElement().getSimpleName().toString());
        }
        return parameterTypeNames;
    }

    public static String spawnSubstitutor(String className, String methodName, List<String> parameterTypeNames, boolean hasReceiver, boolean isVoid) {
        String substitutorName = getClassName(className, methodName);
        StringBuilder classFile = new StringBuilder();
        classFile.append(COPYRIGHT);
        classFile.append(GENERATED_BY).append(className).append("\n\n");
        classFile.append("import ").append(SUBSTITUTION_PACKAGE).append(".").append(className).append(";\n");
        classFile.append(PUBLIC_FINAL_CLASS).append(substitutorName).append(EXTENSION);
        classFile.append(generateInstance(substitutorName)).append("\n");
        classFile.append(generateConstructor(substitutorName)).append("\n");
        classFile.append(generateGetter()).append("\n");
        classFile.append(TAB_1).append(OVERRIDE).append("\n");
        classFile.append(TAB_1).append(PUBLIC_FINAL_OBJECT).append(INVOKE);
        int argIndex = 0;
        if (hasReceiver) {
            classFile.append(extractArg(argIndex++, "StaticObject", TAB_2));
        }
        for (String argType : parameterTypeNames) {
            classFile.append(extractArg(argIndex++, argType, TAB_2));
        }
        if (isVoid) {
            classFile.append(TAB_2).append(extractInvocation(className, methodName, argIndex));
            classFile.append(TAB_2).append("return null\n");
        } else {
            classFile.append(TAB_2).append("return ").append(extractInvocation(className, methodName, argIndex));

        }
        classFile.append(TAB_1).append("}\n");
        classFile.append("}");
        return classFile.toString();
    }

}
