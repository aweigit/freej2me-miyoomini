/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.demo.egl;

import org.lwjgl.egl.*;

import org.lwjgl.opengles.*;
import org.lwjgl.system.*;

import java.lang.reflect.*;
import java.nio.*;

import static org.lwjgl.egl.EGL14.*;

import static org.lwjgl.opengles.GLES20.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class EGLDemo {

    public EGLDemo() {
    }

    public static void main(String[] args) {
    

        int WIDTH  = 300;
        int HEIGHT = 300;



        // EGL capabilities
        long dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);

        EGLCapabilities egl;
        try (MemoryStack stack = stackPush()) {
            IntBuffer major = stack.mallocInt(1);
            IntBuffer minor = stack.mallocInt(1);

            if (!eglInitialize(dpy, major, minor)) {
                throw new IllegalStateException(String.format("Failed to initialize EGL [0x%X]", eglGetError()));
            }

            egl = EGL.createDisplayCapabilities(dpy, major.get(0), minor.get(0));
        }

        try {
            System.out.println("EGL Capabilities:");
            for (Field f : EGLCapabilities.class.getFields()) {
                if (f.getType() == boolean.class) {
                    if (f.get(egl).equals(Boolean.TRUE)) {
                        System.out.println("\t" + f.getName());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        GLESCapabilities gles = GLES.createCapabilities();

        try {
            System.out.println("OpenGL ES Capabilities:");
            for (Field f : GLESCapabilities.class.getFields()) {
                if (f.getType() == boolean.class) {
                    if (f.get(gles).equals(Boolean.TRUE)) {
                        System.out.println("\t" + f.getName());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        System.out.println("GL_VENDOR: " + glGetString(GL_VENDOR));
        System.out.println("GL_VERSION: " + glGetString(GL_VERSION));
        System.out.println("GL_RENDERER: " + glGetString(GL_RENDERER));
		
		System.out.println("GL_error: " +glGetError());


        glClearColor(0.0f, 0.5f, 1.0f, 0.0f);
        

        GLES.setCapabilities(null);

    }

}