/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import javax.swing.JTextArea;

/**
 *
 * @author dmcd2356
 */
public class RunnerThread extends Thread {

    private JTextArea stdout;           // text widget to output stdout & stderr to
    private String[] command;           // the command to execute
    private String workingdir;          // the dir to run the command from
    private OutputStream stdin;
    private ThreadLauncher.ThreadInfo commandInfo; // additional information about the job
    private Process proc;
    private int exitcode;
    private long pid;
    private boolean killProcess;
    private volatile boolean running;
    
    public RunnerThread(ThreadLauncher.ThreadInfo threadInfo, String[] command, String workdir, JTextArea stdout) {
      this.commandInfo = threadInfo;
      this.command = command;
      this.workingdir = workdir;
      this.stdout = stdout;
      this.stdin = null;

      this.proc = null;
      this.killProcess = false;
      this.running = false;
      this.exitcode = -1;
      this.pid = -1;
    }

    @Override
    public void run() {
      try {
        System.out.println("--------------------------------------------------------------------------------");
        if (workingdir == null) {
          System.out.println("RunnerThread running from current directory");
        } else {
          System.out.println("RunnerThread running from path: " + workingdir);
        }
        System.out.println(String.join(" ", command));
        
        running = true;
        exitcode = issueCommand(this.command);
        running = false;
      } catch (InterruptedException e){
        System.err.println("--INTERRUPTED--");
        proc.destroyForcibly();
        running = false;
      } catch (Exception e) {
        System.err.println("ERROR: Failure in issueCommand for: " + this.command[0]);
      }
    }
    
    public OutputStream getStdin() {
      return stdin;
    }
    
    /**
     * kills the current process
     */
    public void killProcess() {
      this.killProcess = true;
      interrupt();
    }
    	
    /**
     * returns the running status of the thread
     * 
     * @return true if running, false if not
     */
    public boolean isRunning() {
      return running;
    }
    	
    /**
     * returns the information about the current job
     * 
     * @return the structure containing info saved when the job was started
     */
    public ThreadLauncher.ThreadInfo getJobInfo() {
      return commandInfo;
    }
    	
    /**
     * returns the exit status of the job
     * 
     * @return exit code returned by the executed command.
     */
    public int getExitCode() {
      return exitcode;
    }
    	
    /**
     * returns the pid of the current job
     * 
     * @return the PID of the current job thread
     */
    public long getJobPid() {
      return pid;
    }
    
    /**
     * returns the pid of the current job
     * 
     * @return the PID of the current job thread
     */
    private synchronized long getPidOfProcess(Process proc) {
      long procpid = -1;

      if (proc == null || proc.getClass() == null)
        return -1;
        
      try {
        if (proc.getClass().getName().equals("java.lang.UNIXProcess")) {
          Field f = proc.getClass().getDeclaredField("pid");
          f.setAccessible(true);
          procpid = f.getLong(proc);
          f.setAccessible(false);
        }
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
        procpid = -1;
      }
      return procpid;
    }

    /**
     * Formats and runs the specified command.
     * 
     * @param command - an array of the command and each argument it requires
     * @return the status of the command (0 = success)
     * @throws Exception 
     */
    private int issueCommand(String[] command) throws InterruptedException, IOException {
      int retcode;
      PrintStream printStream = null;

      // build up the command and argument string
      ProcessBuilder builder = new ProcessBuilder(command);
      if (workingdir != null) {
        File workdir = new File(workingdir);
        if (workdir.isDirectory())
          builder.directory(workdir);
      }
    		
      // re-direct stdout and stderr to the text window
      if (this.stdout != null) {
        // merge stderr into stdout so both go to the specified text area
        builder.redirectErrorStream(true);
        printStream = new PrintStream(new RedirectOutputStream(this.stdout));
        System.setOut(printStream);
        System.setErr(printStream);
      }
        
      System.out.println("--------------------------------------------------------------------------------");
      if (workingdir == null) {
        System.out.println("RunnerThread running from current directory");
      } else {
        System.out.println("RunnerThread running from path: " + workingdir);
      }
      System.out.println(String.join(" ", command));
      proc = builder.start();
      pid = getPidOfProcess(proc);

      // save access to stdin of program
      stdin = proc.getOutputStream();
            
      // run the command
      String status;
      BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      while (proc.isAlive()) {
        while ((status = br.readLine()) != null) {
          System.out.println(status);
        }
        if (killProcess) {
          System.out.println("--KILLING--");
          proc.getInputStream().close();
          proc.getOutputStream().close();	
          proc.getErrorStream().close();
          proc.destroyForcibly();
          stdin = null;
          // flush the output stream, so it isn't sent to next command and restore streams
          if (printStream != null) {
            printStream.flush();
          }
        }
        Thread.sleep(100);
      }

      retcode = proc.exitValue();
      System.out.println("process " + pid + " exit code = " + retcode);
      proc.destroy();
      stdin = null;

      return retcode;
    }
}
