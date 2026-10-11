/** A module declaration for the converter's tests (defmodule). */
@Deprecated
module com.example {
  requires transitive java.logging;
  requires static java.desktop;
  exports com.example.api;
  exports com.example.impl to java.base;
  opens com.example.impl;
  uses com.example.api.Plugin;
  provides com.example.api.Plugin with com.example.impl.DefaultPlugin;
}
