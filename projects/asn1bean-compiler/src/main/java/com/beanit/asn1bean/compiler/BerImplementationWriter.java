package com.beanit.asn1bean.compiler;

import com.beanit.asn1bean.compiler.model.AsnModule;

import java.io.IOException;

public interface BerImplementationWriter {

  void translateModule(AsnModule module) throws IOException;


  void initOutputDir() throws IOException;

 default void addLibSymbols(AsnModule module) {

 }

 default void postOutput() throws IOException {}


  enum TagClass {
    UNIVERSAL,
    APPLICATION,
    CONTEXT,
    PRIVATE
  }

  enum TagType {
    EXPLICIT,
    IMPLICIT
  }

  enum TypeStructure {
    PRIMITIVE,
    CONSTRUCTED
  }

  class Tag {

    public int value;
    public TagClass tagClass;
    public TagType type;
    public TypeStructure typeStructure;
  }
}
