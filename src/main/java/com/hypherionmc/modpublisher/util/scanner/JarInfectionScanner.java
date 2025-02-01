/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.util.scanner;

import com.hypherionmc.modpublisher.util.CommonUtil;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author HypherionSA
 * Contains code copied from https://github.com/MCRcortex/nekodetector/blob/master/src/main/java/me/cortex/jarscanner/Detector.java
 * with permission
 */
@Deprecated
public class JarInfectionScanner {

    public static void scan(Project project, Object file) throws Exception {
        File jarFile = CommonUtil.resolveFile(project, file);

        if (jarFile != null) {
            project.getLogger().lifecycle("Scanning {} for presence of fractureiser", jarFile.getAbsolutePath());
            ZipFile zipFile = new ZipFile(jarFile);
            scan(zipFile, project);
            zipFile.close();
        }
    }

    private static void scan(ZipFile file, Project project) {
        try {
            boolean matches = file.stream()
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .anyMatch(entry -> {
                        try {
                            return scanClass(readAllBytes(file.getInputStream(entry)));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!matches)
                return;
            throw new Exception("!!!! " + file.getName() + " is infected with fractureiser");
        } catch (Exception e) {
            project.getLogger().error("Failed to scan {}", file.getName(), e);
        }
    }

    private static final AbstractInsnNode[] SIG1 = new AbstractInsnNode[] {
            new TypeInsnNode(NEW, "java/lang/String"),
            new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"),
            new TypeInsnNode(NEW, "java/lang/String"),
            new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"),
            new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;"),
            new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getConstructor", "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;"),
            new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"),
            new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"),
            new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"),
            new MethodInsnNode(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V"),
            new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;"),
            new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;"),
            new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"),
            new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"),
            new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"),
    };

    private static final AbstractInsnNode[] SIG2 = new AbstractInsnNode[] {
            new MethodInsnNode(INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;"),
            new MethodInsnNode(INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;"),
            new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "INVOKEVIRTUAL", "(Ljava/lang/String;)Ljava/lang/String;"),//TODO:FIXME: this might not be in all of them
            new MethodInsnNode(INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B"),
            new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"),
            new MethodInsnNode(INVOKEVIRTUAL, "java/io/File", "getPath", "()Ljava/lang/String;"),
            new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Runtime", "exec", "([Ljava/lang/String;)Ljava/lang/Process;"),
    };
    private static boolean same(AbstractInsnNode a, AbstractInsnNode b) {
        if (a instanceof TypeInsnNode) {
            return ((TypeInsnNode)a).desc.equals(((TypeInsnNode)b).desc);
        }
        if (a instanceof MethodInsnNode) {
            return ((MethodInsnNode)a).owner.equals(((MethodInsnNode)b).owner) && ((MethodInsnNode)a).desc.equals(((MethodInsnNode)b).desc);
        }
        if (a instanceof InsnNode) {
            return true;
        }
        throw new IllegalArgumentException("TYPE NOT ADDED");
    }

    private static boolean scanClass(byte[] clazz) {
        ClassReader reader = new ClassReader(clazz);
        ClassNode node = new ClassNode();
        try {
            reader.accept(node, 0);
        } catch (Exception e) {
            return false;//Yes this is very hacky but should never happen with valid clasees
        }
        for (MethodNode method : node.methods) {
            {
                //Method 1, this is a hard detect, if it matches this it is 100% chance infected
                boolean match = true;
                int j = 0;
                for (int i = 0; i < method.instructions.size() && j < SIG1.length; i++) {
                    if (method.instructions.get(i).getOpcode() == -1) {
                        continue;
                    }
                    if (method.instructions.get(i).getOpcode() == SIG1[j].getOpcode()) {
                        if (!same(method.instructions.get(i), SIG1[j++])) {
                            match = false;
                            break;
                        }
                    }
                }
                if (j != SIG1.length) {
                    match = false;
                }
                if (match) {
                    return true;
                }
            }

            {
                //Method 2, this is a near hard detect, if it matches this it is 95% chance infected
                boolean match = false;
                outer:
                for (int q = 0; q < method.instructions.size(); q++) {
                    int j = 0;
                    for (int i = q; i < method.instructions.size() && j < SIG2.length; i++) {
                        if (method.instructions.get(i).getOpcode() != SIG2[j].getOpcode()) {
                            continue;
                        }

                        if (method.instructions.get(i).getOpcode() == SIG2[j].getOpcode()) {
                            if (!same(method.instructions.get(i), SIG2[j++])) {
                                continue outer;
                            }
                        }
                    }
                    if (j == SIG2.length) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 1024;
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                outputStream.write(buf, 0, readLen);

            return outputStream.toByteArray();
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }
}
