/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Azul Systems, Inc. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;

import java.io.*;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import jdk.internal.access.SharedSecrets;
import jdk.internal.loader.NativeLibrary;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

/**
 * 每个java application拥有一个Runtime类的唯一实例，该实例允许application与环境进行交互
 * 可以通过getRuntime方法获取当前运行的实例，application不能创建该类的实例
 */

public class Runtime {
    private static final Runtime currentRuntime = new Runtime();

    private static Version version;

    /**
     * 返回与当前application绑定的Runtime实例，
     * Runtime类的大多数方法都是实例方法，需使用当前Runtime实例对象调用
     */
    public static Runtime getRuntime() {
        return currentRuntime;
    }

    /**
     *  Don't let anyone else instantiate this class
     *  不让其他任何人实例化此类
     * */
    private Runtime() {}

    /**
     * 终止当前运行的JVM，通过启动它的关闭序列 shutdown sequence，此方法不会正常返回
     * 参数：status code 状态码，按照惯例 非0表示异常终止
     *
     * 所有通过addShutdownHook方法注册的shutdown hook，
     * 都会以某种顺序允许并发的执行，直到结束。
     * 一旦这些hook执行结束，JVM将停止运行
     *
     * 如果所有shutdown hook都已经运行完毕，且调用时status code 非0，
     * 则此方法将以给定的status code 终止halts JVM。否则此方法将无限期的阻塞
     *
     * System#exit(int)方法是调用此方法常规且便捷的方式
     */
    public void exit(int status) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkExit(status);
        }
        Shutdown.exit(status);
    }

    /**
     * 注册一个新的shutdown hook。
     *
     * JVM会在响应两种事件时进行关闭shutdown
     * 1. 程序正常退出exits：当最后一个非守护线程退出时，或调用exit（等效System.exit）方法时
     * 2. JVM终止terminated：因为用户中断（如：^c），或系统范围的事件
     *
     * shuthook其实时一个已经初始化但是尚未启动的Thread，当JVM开始它的shutdown流程时，
     * 将以某种未指定的顺序启动所有已经注册的shuthook，并让他们并发运行。
     * 当所有hook都执行完毕后，JVM将停止运行halt。
     * 注意：deamon thread守护线程将在JVM shutdown期间继续运行。
     * 如果是通过调用exit方法，non-deamnon thread非守护线程也将持续运行
     *
     * 一旦shutdown程序启动，不能被停止，除非调用halt方法，将强制terminal终止JVM
     *
     * 一旦shutdown程序启动，无法在注册新的hook或移除已经注册hook，将抛出异常
     *
     * shutdown hook应该以防御性编程，特别是：线程安全、避免死锁，此外hook不能盲目依赖可能已经注册shutdown的服务，因为服务可能处于shutdown
     *
     * shutdown hook应该尽快完成他们的工作，当调用exit方法时，期望JVM能迅速shutdown并exit。
     * 当JVM因为用户注销 logoff或system shutdown系统关闭而terminal终止时，底层系统只会允许在有限时间内完成shutdown并exit
     *
     * shuthook的uncatch exception和其他thread一样，调用ThreadGroup#uncaughtException方法
     * 默认实现为打印exception stack trace到System#err 且 terminal终止线程，不会导致JVM exit或halt
     *
     * 在极少情况下JVM可能会abort中止，即没有正常shutdown的情况下stop running停止运行
     * 通常为外部terminal：如sigkill等
     */
    public void addShutdownHook(Thread hook) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("shutdownHooks"));
        }
        ApplicationShutdownHooks.add(hook);
    }

    /**
     * 取消之前注册的shutdown hook
     */
    public boolean removeShutdownHook(Thread hook) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("shutdownHooks"));
        }
        return ApplicationShutdownHooks.remove(hook);
    }

    /**
     * 强制终止当前JVM，此方法不会正常返回
     *
     * 此方法请谨慎使用，与exit方法不同，此方法不会启动shutdown hooks，
     * 如果shutdown程序已经启动，此方法不会的等待任何正在运行shutdown hook完成工作
     */
    public void halt(int status) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkExit(status);
        }
        Shutdown.beforeHalt();
        Shutdown.halt(status);
    }

    /**
     * Executes the specified string command in a separate process.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code exec(command)}
     * behaves in exactly the same way as the invocation
     * {@link #exec(String, String[], File) exec}{@code (command, null, null)}.
     *
     * @param   command   a specified system command.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code command} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If {@code command} is empty
     *
     * @see     #exec(String[], String[], File)
     * @see     ProcessBuilder
     */
    public Process exec(String command) throws IOException {
        return exec(command, null, null);
    }

    /**
     * Executes the specified string command in a separate process with the
     * specified environment.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code exec(command, envp)}
     * behaves in exactly the same way as the invocation
     * {@link #exec(String, String[], File) exec}{@code (command, envp, null)}.
     *
     * @param   command   a specified system command.
     *
     * @param   envp      array of strings, each element of which
     *                    has environment variable settings in the format
     *                    <i>name</i>=<i>value</i>, or
     *                    {@code null} if the subprocess should inherit
     *                    the environment of the current process.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code command} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If {@code command} is empty
     *
     * @see     #exec(String[], String[], File)
     * @see     ProcessBuilder
     */
    public Process exec(String command, String[] envp) throws IOException {
        return exec(command, envp, null);
    }

    /**
     * Executes the specified string command in a separate process with the
     * specified environment and working directory.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code exec(command, envp, dir)}
     * behaves in exactly the same way as the invocation
     * {@link #exec(String[], String[], File) exec}{@code (cmdarray, envp, dir)},
     * where {@code cmdarray} is an array of all the tokens in
     * {@code command}.
     *
     * <p>More precisely, the {@code command} string is broken
     * into tokens using a {@link StringTokenizer} created by the call
     * {@code new StringTokenizer(command)} with no
     * further modification of the character categories.  The tokens
     * produced by the tokenizer are then placed in the new string
     * array {@code cmdarray}, in the same order.
     *
     * @param   command   a specified system command.
     *
     * @param   envp      array of strings, each element of which
     *                    has environment variable settings in the format
     *                    <i>name</i>=<i>value</i>, or
     *                    {@code null} if the subprocess should inherit
     *                    the environment of the current process.
     *
     * @param   dir       the working directory of the subprocess, or
     *                    {@code null} if the subprocess should inherit
     *                    the working directory of the current process.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code command} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If {@code command} is empty
     *
     * @see     ProcessBuilder
     * @since 1.3
     */
    public Process exec(String command, String[] envp, File dir)
        throws IOException {
        if (command.isEmpty())
            throw new IllegalArgumentException("Empty command");

        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdarray[i] = st.nextToken();
        return exec(cmdarray, envp, dir);
    }

    /**
     * Executes the specified command and arguments in a separate process.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code exec(cmdarray)}
     * behaves in exactly the same way as the invocation
     * {@link #exec(String[], String[], File) exec}{@code (cmdarray, null, null)}.
     *
     * @param   cmdarray  array containing the command to call and
     *                    its arguments.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code cmdarray} is {@code null},
     *          or one of the elements of {@code cmdarray} is {@code null}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code cmdarray} is an empty array
     *          (has length {@code 0})
     *
     * @see     ProcessBuilder
     */
    public Process exec(String cmdarray[]) throws IOException {
        return exec(cmdarray, null, null);
    }

    /**
     * Executes the specified command and arguments in a separate process
     * with the specified environment.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code exec(cmdarray, envp)}
     * behaves in exactly the same way as the invocation
     * {@link #exec(String[], String[], File) exec}{@code (cmdarray, envp, null)}.
     *
     * @param   cmdarray  array containing the command to call and
     *                    its arguments.
     *
     * @param   envp      array of strings, each element of which
     *                    has environment variable settings in the format
     *                    <i>name</i>=<i>value</i>, or
     *                    {@code null} if the subprocess should inherit
     *                    the environment of the current process.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code cmdarray} is {@code null},
     *          or one of the elements of {@code cmdarray} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code cmdarray} is an empty array
     *          (has length {@code 0})
     *
     * @see     ProcessBuilder
     */
    public Process exec(String[] cmdarray, String[] envp) throws IOException {
        return exec(cmdarray, envp, null);
    }


    /**
     * Executes the specified command and arguments in a separate process with
     * the specified environment and working directory.
     *
     * <p>Given an array of strings {@code cmdarray}, representing the
     * tokens of a command line, and an array of strings {@code envp},
     * representing "environment" variable settings, this method creates
     * a new process in which to execute the specified command.
     *
     * <p>This method checks that {@code cmdarray} is a valid operating
     * system command.  Which commands are valid is system-dependent,
     * but at the very least the command must be a non-empty list of
     * non-null strings.
     *
     * <p>If {@code envp} is {@code null}, the subprocess inherits the
     * environment settings of the current process.
     *
     * <p>A minimal set of system dependent environment variables may
     * be required to start a process on some operating systems.
     * As a result, the subprocess may inherit additional environment variable
     * settings beyond those in the specified environment.
     *
     * <p>{@link ProcessBuilder#start()} is now the preferred way to
     * start a process with a modified environment.
     *
     * <p>The working directory of the new subprocess is specified by {@code dir}.
     * If {@code dir} is {@code null}, the subprocess inherits the
     * current working directory of the current process.
     *
     * <p>If a security manager exists, its
     * {@link SecurityManager#checkExec checkExec}
     * method is invoked with the first component of the array
     * {@code cmdarray} as its argument. This may result in a
     * {@link SecurityException} being thrown.
     *
     * <p>Starting an operating system process is highly system-dependent.
     * Among the many things that can go wrong are:
     * <ul>
     * <li>The operating system program file was not found.
     * <li>Access to the program file was denied.
     * <li>The working directory does not exist.
     * </ul>
     *
     * <p>In such cases an exception will be thrown.  The exact nature
     * of the exception is system-dependent, but it will always be a
     * subclass of {@link IOException}.
     *
     * <p>If the operating system does not support the creation of
     * processes, an {@link UnsupportedOperationException} will be thrown.
     *
     *
     * @param   cmdarray  array containing the command to call and
     *                    its arguments.
     *
     * @param   envp      array of strings, each element of which
     *                    has environment variable settings in the format
     *                    <i>name</i>=<i>value</i>, or
     *                    {@code null} if the subprocess should inherit
     *                    the environment of the current process.
     *
     * @param   dir       the working directory of the subprocess, or
     *                    {@code null} if the subprocess should inherit
     *                    the working directory of the current process.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  UnsupportedOperationException
     *          If the operating system does not support the creation of processes.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code cmdarray} is {@code null},
     *          or one of the elements of {@code cmdarray} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code cmdarray} is an empty array
     *          (has length {@code 0})
     *
     * @see     ProcessBuilder
     * @since 1.3
     */
    public Process exec(String[] cmdarray, String[] envp, File dir)
        throws IOException {
        return new ProcessBuilder(cmdarray)
            .environment(envp)
            .directory(dir)
            .start();
    }

    /**
     * Returns the number of processors available to the Java virtual machine.
     *
     * <p> This value may change during a particular invocation of the virtual
     * machine.  Applications that are sensitive to the number of available
     * processors should therefore occasionally poll this property and adjust
     * their resource usage appropriately. </p>
     *
     * @return  the maximum number of processors available to the virtual
     *          machine; never smaller than one
     * @since 1.4
     */
    public native int availableProcessors();

    /**
     * Returns the amount of free memory in the Java Virtual Machine.
     * Calling the
     * {@code gc} method may result in increasing the value returned
     * by {@code freeMemory.}
     *
     * @return  an approximation to the total amount of memory currently
     *          available for future allocated objects, measured in bytes.
     */
    public native long freeMemory();

    /**
     * Returns the total amount of memory in the Java virtual machine.
     * The value returned by this method may vary over time, depending on
     * the host environment.
     * <p>
     * Note that the amount of memory required to hold an object of any
     * given type may be implementation-dependent.
     *
     * @return  the total amount of memory currently available for current
     *          and future objects, measured in bytes.
     */
    public native long totalMemory();

    /**
     * Returns the maximum amount of memory that the Java virtual machine
     * will attempt to use.  If there is no inherent limit then the value
     * {@link java.lang.Long#MAX_VALUE} will be returned.
     *
     * @return  the maximum amount of memory that the virtual machine will
     *          attempt to use, measured in bytes
     * @since 1.4
     */
    public native long maxMemory();

    /**
     * Runs the garbage collector in the Java Virtual Machine.
     * <p>
     * Calling this method suggests that the Java Virtual Machine
     * expend effort toward recycling unused objects in order to
     * make the memory they currently occupy available for reuse
     * by the Java Virtual Machine.
     * When control returns from the method call, the Java Virtual Machine
     * has made a best effort to reclaim space from all unused objects.
     * There is no guarantee that this effort will recycle any particular
     * number of unused objects, reclaim any particular amount of space, or
     * complete at any particular time, if at all, before the method returns or ever.
     * There is also no guarantee that this effort will determine
     * the change of reachability in any particular number of objects,
     * or that any particular number of {@link java.lang.ref.Reference Reference}
     * objects will be cleared and enqueued.
     * <p>
     * The name {@code gc} stands for "garbage
     * collector". The Java Virtual Machine performs this recycling
     * process automatically as needed, in a separate thread, even if the
     * {@code gc} method is not invoked explicitly.
     * <p>
     * The method {@link System#gc()} is the conventional and convenient
     * means of invoking this method.
     */
    public native void gc();

    /**
     * Runs the finalization methods of any objects pending finalization.
     * Calling this method suggests that the Java virtual machine expend
     * effort toward running the {@code finalize} methods of objects
     * that have been found to be discarded but whose {@code finalize}
     * methods have not yet been run. When control returns from the
     * method call, the virtual machine has made a best effort to
     * complete all outstanding finalizations.
     * <p>
     * The virtual machine performs the finalization process
     * automatically as needed, in a separate thread, if the
     * {@code runFinalization} method is not invoked explicitly.
     * <p>
     * The method {@link System#runFinalization()} is the conventional
     * and convenient means of invoking this method.
     *
     * @see     java.lang.Object#finalize()
     */
    public void runFinalization() {
        SharedSecrets.getJavaLangRefAccess().runFinalization();
    }

    /**
     * Loads the native library specified by the filename argument.  The filename
     * argument must be an absolute path name.
     * (for example
     * {@code Runtime.getRuntime().load("/home/avh/lib/libX11.so");}).
     *
     * If the filename argument, when stripped of any platform-specific library
     * prefix, path, and file extension, indicates a library whose name is,
     * for example, L, and a native library called L is statically linked
     * with the VM, then the JNI_OnLoad_L function exported by the library
     * is invoked rather than attempting to load a dynamic library.
     * A filename matching the argument does not have to exist in the file
     * system.
     * See the <a href="{@docRoot}/../specs/jni/index.html"> JNI Specification</a>
     * for more details.
     *
     * Otherwise, the filename argument is mapped to a native library image in
     * an implementation-dependent manner.
     * <p>
     * First, if there is a security manager, its {@code checkLink}
     * method is called with the {@code filename} as its argument.
     * This may result in a security exception.
     * <p>
     * This is similar to the method {@link #loadLibrary(String)}, but it
     * accepts a general file name as an argument rather than just a library
     * name, allowing any file of native code to be loaded.
     * <p>
     * The method {@link System#load(String)} is the conventional and
     * convenient means of invoking this method.
     *
     * @param      filename   the file to load.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkLink} method doesn't allow
     *             loading of the specified dynamic library
     * @throws     UnsatisfiedLinkError  if either the filename is not an
     *             absolute path name, the native library is not statically
     *             linked with the VM, or the library cannot be mapped to
     *             a native library image by the host system.
     * @throws     NullPointerException if {@code filename} is
     *             {@code null}
     * @see        java.lang.Runtime#getRuntime()
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkLink(java.lang.String)
     */
    @CallerSensitive
    public void load(String filename) {
        load0(Reflection.getCallerClass(), filename);
    }

    void load0(Class<?> fromClass, String filename) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkLink(filename);
        }
        File file = new File(filename);
        if (!file.isAbsolute()) {
            throw new UnsatisfiedLinkError(
                "Expecting an absolute path of the library: " + filename);
        }
        ClassLoader.loadLibrary(fromClass, file);
    }

    /**
     * Loads the native library specified by the {@code libname}
     * argument.  The {@code libname} argument must not contain any platform
     * specific prefix, file extension or path. If a native library
     * called {@code libname} is statically linked with the VM, then the
     * JNI_OnLoad_{@code libname} function exported by the library is invoked.
     * See the <a href="{@docRoot}/../specs/jni/index.html"> JNI Specification</a>
     * for more details.
     *
     * Otherwise, the libname argument is loaded from a system library
     * location and mapped to a native library image in an
     * implementation-dependent manner.
     * <p>
     * First, if there is a security manager, its {@code checkLink}
     * method is called with the {@code libname} as its argument.
     * This may result in a security exception.
     * <p>
     * The method {@link System#loadLibrary(String)} is the conventional
     * and convenient means of invoking this method. If native
     * methods are to be used in the implementation of a class, a standard
     * strategy is to put the native code in a library file (call it
     * {@code LibFile}) and then to put a static initializer:
     * <blockquote><pre>
     * static { System.loadLibrary("LibFile"); }
     * </pre></blockquote>
     * within the class declaration. When the class is loaded and
     * initialized, the necessary native code implementation for the native
     * methods will then be loaded as well.
     * <p>
     * If this method is called more than once with the same library
     * name, the second and subsequent calls are ignored.
     *
     * @param      libname   the name of the library.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkLink} method doesn't allow
     *             loading of the specified dynamic library
     * @throws     UnsatisfiedLinkError if either the libname argument
     *             contains a file path, the native library is not statically
     *             linked with the VM,  or the library cannot be mapped to a
     *             native library image by the host system.
     * @throws     NullPointerException if {@code libname} is
     *             {@code null}
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkLink(java.lang.String)
     */
    @CallerSensitive
    public void loadLibrary(String libname) {
        loadLibrary0(Reflection.getCallerClass(), libname);
    }

    void loadLibrary0(Class<?> fromClass, String libname) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkLink(libname);
        }
        if (libname.indexOf((int)File.separatorChar) != -1) {
            throw new UnsatisfiedLinkError(
                "Directory separator should not appear in library name: " + libname);
        }
        ClassLoader.loadLibrary(fromClass, libname);
    }

    /**
     * Returns the version of the Java Runtime Environment as a {@link Version}.
     *
     * @return  the {@link Version} of the Java Runtime Environment
     *
     * @since  9
     */
    public static Version version() {
        var v = version;
        if (v == null) {
            v = new Version(VersionProps.versionNumbers(),
                    VersionProps.pre(), VersionProps.build(),
                    VersionProps.optional());
            version = v;
        }
        return v;
    }

    /**
     * A representation of a version string for an implementation of the
     * Java&nbsp;SE Platform.  A version string consists of a version number
     * optionally followed by pre-release and build information.
     *
     * <h2><a id="verNum">Version numbers</a></h2>
     *
     * <p> A <em>version number</em>, {@code $VNUM}, is a non-empty sequence of
     * elements separated by period characters (U+002E).  An element is either
     * zero, or an unsigned integer numeral without leading zeros.  The final
     * element in a version number must not be zero.  When an element is
     * incremented, all subsequent elements are removed.  The format is: </p>
     *
     * <blockquote><pre>
     * [1-9][0-9]*((\.0)*\.[1-9][0-9]*)*
     * </pre></blockquote>
     *
     * <p> The sequence may be of arbitrary length but the first four elements
     * are assigned specific meanings, as follows:</p>
     *
     * <blockquote><pre>
     * $FEATURE.$INTERIM.$UPDATE.$PATCH
     * </pre></blockquote>
     *
     * <ul>
     *
     * <li><p> <a id="FEATURE">{@code $FEATURE}</a> &#x2014; The
     * feature-release counter, incremented for every feature release
     * regardless of release content.  Features may be added in a feature
     * release; they may also be removed, if advance notice was given at least
     * one feature release ahead of time.  Incompatible changes may be made
     * when justified. </p></li>
     *
     * <li><p> <a id="INTERIM">{@code $INTERIM}</a> &#x2014; The
     * interim-release counter, incremented for non-feature releases that
     * contain compatible bug fixes and enhancements but no incompatible
     * changes, no feature removals, and no changes to standard APIs.
     * </p></li>
     *
     * <li><p> <a id="UPDATE">{@code $UPDATE}</a> &#x2014; The update-release
     * counter, incremented for compatible update releases that fix security
     * issues, regressions, and bugs in newer features. </p></li>
     *
     * <li><p> <a id="PATCH">{@code $PATCH}</a> &#x2014; The emergency
     * patch-release counter, incremented only when it's necessary to produce
     * an emergency release to fix a critical issue. </p></li>
     *
     * </ul>
     *
     * <p> The fifth and later elements of a version number are free for use by
     * platform implementors, to identify implementor-specific patch
     * releases. </p>
     *
     * <p> A version number never has trailing zero elements.  If an element
     * and all those that follow it logically have the value zero then all of
     * them are omitted. </p>
     *
     * <p> The sequence of numerals in a version number is compared to another
     * such sequence in numerical, pointwise fashion; <em>e.g.</em>, {@code
     * 10.0.4} is less than {@code 10.1.2}.  If one sequence is shorter than
     * another then the missing elements of the shorter sequence are considered
     * to be less than the corresponding elements of the longer sequence;
     * <em>e.g.</em>, {@code 10.0.2} is less than {@code 10.0.2.1}. </p>
     *
     * <h2><a id="verStr">Version strings</a></h2>
     *
     * <p> A <em>version string</em>, {@code $VSTR}, is a version number {@code
     * $VNUM}, as described above, optionally followed by pre-release and build
     * information, in one of the following formats: </p>
     *
     * <blockquote><pre>
     *     $VNUM(-$PRE)?\+$BUILD(-$OPT)?
     *     $VNUM-$PRE(-$OPT)?
     *     $VNUM(\+-$OPT)?
     * </pre></blockquote>
     *
     * <p> where: </p>
     *
     * <ul>
     *
     * <li><p> <a id="pre">{@code $PRE}</a>, matching {@code ([a-zA-Z0-9]+)}
     * &#x2014; A pre-release identifier.  Typically {@code ea}, for a
     * potentially unstable early-access release under active development, or
     * {@code internal}, for an internal developer build. </p></li>
     *
     * <li><p> <a id="build">{@code $BUILD}</a>, matching {@code
     * (0|[1-9][0-9]*)} &#x2014; The build number, incremented for each promoted
     * build.  {@code $BUILD} is reset to {@code 1} when any portion of {@code
     * $VNUM} is incremented. </p></li>
     *
     * <li><p> <a id="opt">{@code $OPT}</a>, matching {@code ([-a-zA-Z0-9.]+)}
     * &#x2014; Additional build information, if desired.  In the case of an
     * {@code internal} build this will often contain the date and time of the
     * build. </p></li>
     *
     * </ul>
     *
     * <p> A version string {@code 10-ea} matches {@code $VNUM = "10"} and
     * {@code $PRE = "ea"}.  The version string {@code 10+-ea} matches
     * {@code $VNUM = "10"} and {@code $OPT = "ea"}. </p>
     *
     * <p> When comparing two version strings, the value of {@code $OPT}, if
     * present, may or may not be significant depending on the chosen
     * comparison method.  The comparison methods {@link #compareTo(Version)
     * compareTo()} and {@link #compareToIgnoreOptional(Version)
     * compareToIgnoreOptional()} should be used consistently with the
     * corresponding methods {@link #equals(Object) equals()} and {@link
     * #equalsIgnoreOptional(Object) equalsIgnoreOptional()}.  </p>
     *
     * <p> A <em>short version string</em>, {@code $SVSTR}, often useful in
     * less formal contexts, is a version number optionally followed by a
     * pre-release identifier:</p>
     *
     * <blockquote><pre>
     *     $VNUM(-$PRE)?
     * </pre></blockquote>
     *
     * <p>This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
     * class; programmers should treat instances that are
     * {@linkplain #equals(Object) equal} as interchangeable and should not
     * use instances for synchronization, or unpredictable behavior may
     * occur. For example, in a future release, synchronization may fail.</p>
     *
     * @since  9
     */
    @jdk.internal.ValueBased
    public static final class Version
        implements Comparable<Version>
    {
        private final List<Integer>     version;
        private final Optional<String>  pre;
        private final Optional<Integer> build;
        private final Optional<String>  optional;

        /*
         * List of version number components passed to this constructor MUST
         * be at least unmodifiable (ideally immutable). In the case of an
         * unmodifiable list, the caller MUST hand the list over to this
         * constructor and never change the underlying list.
         */
        private Version(List<Integer> unmodifiableListOfVersions,
                        Optional<String> pre,
                        Optional<Integer> build,
                        Optional<String> optional)
        {
            this.version = unmodifiableListOfVersions;
            this.pre = pre;
            this.build = build;
            this.optional = optional;
        }

        /**
         * Parses the given string as a valid
         * <a href="#verStr">version string</a> containing a
         * <a href="#verNum">version number</a> followed by pre-release and
         * build information.
         *
         * @param  s
         *         A string to interpret as a version
         *
         * @throws  IllegalArgumentException
         *          If the given string cannot be interpreted as a valid
         *          version
         *
         * @throws  NullPointerException
         *          If the given string is {@code null}
         *
         * @throws  NumberFormatException
         *          If an element of the version number or the build number
         *          cannot be represented as an {@link Integer}
         *
         * @return  The Version of the given string
         */
        public static Version parse(String s) {
            if (s == null)
                throw new NullPointerException();

            // Shortcut to avoid initializing VersionPattern when creating
            // feature-version constants during startup
            if (isSimpleNumber(s)) {
                return new Version(List.of(Integer.parseInt(s)),
                        Optional.empty(), Optional.empty(), Optional.empty());
            }
            Matcher m = VersionPattern.VSTR_PATTERN.matcher(s);
            if (!m.matches())
                throw new IllegalArgumentException("Invalid version string: '"
                                                   + s + "'");

            // $VNUM is a dot-separated list of integers of arbitrary length
            String[] split = m.group(VersionPattern.VNUM_GROUP).split("\\.");
            Integer[] version = new Integer[split.length];
            for (int i = 0; i < split.length; i++) {
                version[i] = Integer.parseInt(split[i]);
            }

            Optional<String> pre = Optional.ofNullable(
                    m.group(VersionPattern.PRE_GROUP));

            String b = m.group(VersionPattern.BUILD_GROUP);
            // $BUILD is an integer
            Optional<Integer> build = (b == null)
                ? Optional.empty()
                : Optional.of(Integer.parseInt(b));

            Optional<String> optional = Optional.ofNullable(
                    m.group(VersionPattern.OPT_GROUP));

            // empty '+'
            if (!build.isPresent()) {
                if (m.group(VersionPattern.PLUS_GROUP) != null) {
                    if (optional.isPresent()) {
                        if (pre.isPresent())
                            throw new IllegalArgumentException("'+' found with"
                                + " pre-release and optional components:'" + s
                                + "'");
                    } else {
                        throw new IllegalArgumentException("'+' found with neither"
                            + " build or optional components: '" + s + "'");
                    }
                } else {
                    if (optional.isPresent() && !pre.isPresent()) {
                        throw new IllegalArgumentException("optional component"
                            + " must be preceded by a pre-release component"
                            + " or '+': '" + s + "'");
                    }
                }
            }
            return new Version(List.of(version), pre, build, optional);
        }

        private static boolean isSimpleNumber(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                char lowerBound = (i > 0) ? '0' : '1';
                if (c < lowerBound || c > '9') {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns the value of the <a href="#FEATURE">feature</a> element of
         * the version number.
         *
         * @return The value of the feature element
         *
         * @since 10
         */
        public int feature() {
            return version.get(0);
        }

        /**
         * Returns the value of the <a href="#INTERIM">interim</a> element of
         * the version number, or zero if it is absent.
         *
         * @return The value of the interim element, or zero
         *
         * @since 10
         */
        public int interim() {
            return (version.size() > 1 ? version.get(1) : 0);
        }

        /**
         * Returns the value of the <a href="#UPDATE">update</a> element of the
         * version number, or zero if it is absent.
         *
         * @return The value of the update element, or zero
         *
         * @since 10
         */
        public int update() {
            return (version.size() > 2 ? version.get(2) : 0);
        }

        /**
         * Returns the value of the <a href="#PATCH">patch</a> element of the
         * version number, or zero if it is absent.
         *
         * @return The value of the patch element, or zero
         *
         * @since 10
         */
        public int patch() {
            return (version.size() > 3 ? version.get(3) : 0);
        }

        /**
         * Returns the value of the major element of the version number.
         *
         * @deprecated As of Java&nbsp;SE 10, the first element of a version
         * number is not the major-release number but the feature-release
         * counter, incremented for every time-based release.  Use the {@link
         * #feature()} method in preference to this method.  For compatibility,
         * this method returns the value of the <a href="#FEATURE">feature</a>
         * element.
         *
         * @return The value of the feature element
         */
        @Deprecated(since = "10")
        public int major() {
            return feature();
        }

        /**
         * Returns the value of the minor element of the version number, or
         * zero if it is absent.
         *
         * @deprecated As of Java&nbsp;SE 10, the second element of a version
         * number is not the minor-release number but the interim-release
         * counter, incremented for every interim release.  Use the {@link
         * #interim()} method in preference to this method.  For compatibility,
         * this method returns the value of the <a href="#INTERIM">interim</a>
         * element, or zero if it is absent.
         *
         * @return The value of the interim element, or zero
         */
        @Deprecated(since = "10")
        public int minor() {
            return interim();
        }

        /**
         * Returns the value of the security element of the version number, or
         * zero if it is absent.
         *
         * @deprecated As of Java&nbsp;SE 10, the third element of a version
         * number is not the security level but the update-release counter,
         * incremented for every update release.  Use the {@link #update()}
         * method in preference to this method.  For compatibility, this method
         * returns the value of the <a href="#UPDATE">update</a> element, or
         * zero if it is absent.
         *
         * @return  The value of the update element, or zero
         */
        @Deprecated(since = "10")
        public int security() {
            return update();
        }

        /**
         * Returns an unmodifiable {@link java.util.List List} of the integers
         * represented in the <a href="#verNum">version number</a>.
         * The {@code List} always contains at least one element corresponding to
         * the <a href="#FEATURE">feature version number</a>.
         *
         * @return  An unmodifiable list of the integers
         *          represented in the version number
         */
        public List<Integer> version() {
            return version;
        }

        /**
         * Returns the optional <a href="#pre">pre-release</a> information.
         *
         * @return  The optional pre-release information as a String
         */
        public Optional<String> pre() {
            return pre;
        }

        /**
         * Returns the <a href="#build">build number</a>.
         *
         * @return  The optional build number.
         */
        public Optional<Integer> build() {
            return build;
        }

        /**
         * Returns <a href="#opt">optional</a> additional identifying build
         * information.
         *
         * @return  Additional build information as a String
         */
        public Optional<String> optional() {
            return optional;
        }

        /**
         * Compares this version to another.
         *
         * <p> Each of the components in the <a href="#verStr">version</a> is
         * compared in the following order of precedence: version numbers,
         * pre-release identifiers, build numbers, optional build information.
         * </p>
         *
         * <p> Comparison begins by examining the sequence of version numbers.
         * If one sequence is shorter than another, then the missing elements
         * of the shorter sequence are considered to be less than the
         * corresponding elements of the longer sequence. </p>
         *
         * <p> A version with a pre-release identifier is always considered to
         * be less than a version without one.  Pre-release identifiers are
         * compared numerically when they consist only of digits, and
         * lexicographically otherwise.  Numeric identifiers are considered to
         * be less than non-numeric identifiers.  </p>
         *
         * <p> A version without a build number is always less than one with a
         * build number; otherwise build numbers are compared numerically. </p>
         *
         * <p> The optional build information is compared lexicographically.
         * During this comparison, a version with optional build information is
         * considered to be greater than a version without one. </p>
         *
         * @param  obj
         *         The object to be compared
         *
         * @return  A negative integer, zero, or a positive integer if this
         *          {@code Version} is less than, equal to, or greater than the
         *          given {@code Version}
         *
         * @throws  NullPointerException
         *          If the given object is {@code null}
         */
        @Override
        public int compareTo(Version obj) {
            return compare(obj, false);
        }

        /**
         * Compares this version to another disregarding optional build
         * information.
         *
         * <p> Two versions are compared by examining the version string as
         * described in {@link #compareTo(Version)} with the exception that the
         * optional build information is always ignored. </p>
         *
         * <p> This method provides ordering which is consistent with
         * {@code equalsIgnoreOptional()}. </p>
         *
         * @param  obj
         *         The object to be compared
         *
         * @return  A negative integer, zero, or a positive integer if this
         *          {@code Version} is less than, equal to, or greater than the
         *          given {@code Version}
         *
         * @throws  NullPointerException
         *          If the given object is {@code null}
         */
        public int compareToIgnoreOptional(Version obj) {
            return compare(obj, true);
        }

        private int compare(Version obj, boolean ignoreOpt) {
            if (obj == null)
                throw new NullPointerException();

            int ret = compareVersion(obj);
            if (ret != 0)
                return ret;

            ret = comparePre(obj);
            if (ret != 0)
                return ret;

            ret = compareBuild(obj);
            if (ret != 0)
                return ret;

            if (!ignoreOpt)
                return compareOptional(obj);

            return 0;
        }

        private int compareVersion(Version obj) {
            int size = version.size();
            int oSize = obj.version().size();
            int min = Math.min(size, oSize);
            for (int i = 0; i < min; i++) {
                int val = version.get(i);
                int oVal = obj.version().get(i);
                if (val != oVal)
                    return val - oVal;
            }
            return size - oSize;
        }

        private int comparePre(Version obj) {
            Optional<String> oPre = obj.pre();
            if (!pre.isPresent()) {
                if (oPre.isPresent())
                    return 1;
            } else {
                if (!oPre.isPresent())
                    return -1;
                String val = pre.get();
                String oVal = oPre.get();
                if (val.matches("\\d+")) {
                    return (oVal.matches("\\d+")
                        ? (new BigInteger(val)).compareTo(new BigInteger(oVal))
                        : -1);
                } else {
                    return (oVal.matches("\\d+")
                        ? 1
                        : val.compareTo(oVal));
                }
            }
            return 0;
        }

        private int compareBuild(Version obj) {
            Optional<Integer> oBuild = obj.build();
            if (oBuild.isPresent()) {
                return (build.isPresent()
                        ? build.get().compareTo(oBuild.get())
                        : -1);
            } else if (build.isPresent()) {
                return 1;
            }
            return 0;
        }

        private int compareOptional(Version obj) {
            Optional<String> oOpt = obj.optional();
            if (!optional.isPresent()) {
                if (oOpt.isPresent())
                    return -1;
            } else {
                if (!oOpt.isPresent())
                    return 1;
                return optional.get().compareTo(oOpt.get());
            }
            return 0;
        }

        /**
         * Returns a string representation of this version.
         *
         * @return  The version string
         */
        @Override
        public String toString() {
            StringBuilder sb
                = new StringBuilder(version.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(".")));

            pre.ifPresent(v -> sb.append("-").append(v));

            if (build.isPresent()) {
                sb.append("+").append(build.get());
                if (optional.isPresent())
                    sb.append("-").append(optional.get());
            } else {
                if (optional.isPresent()) {
                    sb.append(pre.isPresent() ? "-" : "+-");
                    sb.append(optional.get());
                }
            }

            return sb.toString();
        }

        /**
         * Determines whether this {@code Version} is equal to another object.
         *
         * <p> Two {@code Version}s are equal if and only if they represent the
         * same version string.
         *
         * @param  obj
         *         The object to which this {@code Version} is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a {@code
         *          Version} that is identical to this {@code Version}
         *
         */
        @Override
        public boolean equals(Object obj) {
            boolean ret = equalsIgnoreOptional(obj);
            if (!ret)
                return false;

            Version that = (Version)obj;
            return (this.optional().equals(that.optional()));
        }

        /**
         * Determines whether this {@code Version} is equal to another
         * disregarding optional build information.
         *
         * <p> Two {@code Version}s are equal if and only if they represent the
         * same version string disregarding the optional build information.
         *
         * @param  obj
         *         The object to which this {@code Version} is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a {@code
         *          Version} that is identical to this {@code Version}
         *          ignoring the optional build information
         *
         */
        public boolean equalsIgnoreOptional(Object obj) {
            if (this == obj)
                return true;
            return (obj instanceof Version that)
                && (this.version().equals(that.version())
                && this.pre().equals(that.pre())
                && this.build().equals(that.build()));
        }

        /**
         * Returns the hash code of this version.
         *
         * @return  The hashcode of this version
         */
        @Override
        public int hashCode() {
            int h = 1;
            int p = 17;

            h = p * h + version.hashCode();
            h = p * h + pre.hashCode();
            h = p * h + build.hashCode();
            h = p * h + optional.hashCode();

            return h;
        }
    }

    private static class VersionPattern {
        // $VNUM(-$PRE)?(\+($BUILD)?(\-$OPT)?)?
        // RE limits the format of version strings
        // ([1-9][0-9]*(?:(?:\.0)*\.[1-9][0-9]*)*)(?:-([a-zA-Z0-9]+))?(?:(\+)(0|[1-9][0-9]*)?)?(?:-([-a-zA-Z0-9.]+))?

        private static final String VNUM
            = "(?<VNUM>[1-9][0-9]*(?:(?:\\.0)*\\.[1-9][0-9]*)*)";
        private static final String PRE      = "(?:-(?<PRE>[a-zA-Z0-9]+))?";
        private static final String BUILD
            = "(?:(?<PLUS>\\+)(?<BUILD>0|[1-9][0-9]*)?)?";
        private static final String OPT      = "(?:-(?<OPT>[-a-zA-Z0-9.]+))?";
        private static final String VSTR_FORMAT = VNUM + PRE + BUILD + OPT;

        static final Pattern VSTR_PATTERN = Pattern.compile(VSTR_FORMAT);

        static final String VNUM_GROUP  = "VNUM";
        static final String PRE_GROUP   = "PRE";
        static final String PLUS_GROUP  = "PLUS";
        static final String BUILD_GROUP = "BUILD";
        static final String OPT_GROUP   = "OPT";
    }
}
