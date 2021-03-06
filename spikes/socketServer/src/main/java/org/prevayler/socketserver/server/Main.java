package org.prevayler.socketserver.server;

/*
 * prevayler.socketServer, a socket-based server (and client library)
 * to help create client-server Prevayler applications
 * 
 * Copyright (C) 2003 Advanced Systems Concepts, Inc.
 * 
 * Written by David Orme <daveo@swtworkbench.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;
import org.prevayler.socketserver.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * This is where the server starts everything
 *
 * @author DaveO
 */
public class Main {

  private static SnapshotThread<Object> snapshotThread;
  private static Prevayler<Object> prevayler;

  private static int port;

  // Init the Prevayler persistence engine and fork snapshot thread
  private static void initPrevayler() throws Exception {
    // Set up the repository location
    //String prevalenceBase = System.getProperty("user.dir") + "/prevalenceBase";
    String prevalenceBase = ServerConfig.properties.getProperty("Repository");
    Log.message("Snapshot/log file dir: " + prevalenceBase);

    // Set up the default port
    port = Integer.parseInt(ServerConfig.properties.getProperty("BasePort"));

    // Set up the root object class
    String rootObjectClassName = ServerConfig.properties.getProperty("RootObjectClass");
    Class<?> rootObjectClass = Class.forName(rootObjectClassName);

    // Create an instance of the root object class and start the server
    //prevayler = PrevaylerFactory.createPrevayler(rootObjectClass.getDeclaredConstructor().newInstance(), prevalenceBase);
    PrevaylerFactory<Object> factory = new PrevaylerFactory<Object>();
    factory.configurePrevalentSystem(rootObjectClass.getDeclaredConstructor().newInstance());
    factory.configurePrevalenceDirectory(prevalenceBase);
    prevayler = factory.create();
    snapshotThread = new SnapshotThread<Object>(prevayler);
    snapshotThread.start();
  }

  private static void runNotificationServer() {
    new Notification(port + 1).start();
  }

  // Command-processing socket server is here
  private static void runCommandServer() throws Exception {
    ServerSocket ss = null;
    boolean listening = true;

    // Listen dynamically
    try {
      ss = new ServerSocket(port);
    } catch (IOException e) {
      Log.error(e, "Couldn't open command server port: " + port);
      System.exit(-1);
    }

    while (listening)
      new CommandThread<Object>(prevayler, ss.accept()).start();

    ss.close();
  }

  // Everything starts here
  public static void main(String[] args) {
    try {
      new ServerConfig();
      initPrevayler();
      runNotificationServer();
      runCommandServer();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

