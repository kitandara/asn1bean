package com.beanit.asn1bean.compiler;

import com.beanit.asn1bean.compiler.model.AsnModule;

import java.util.HashMap;

public class BerGoLangStructWriter extends BerJavaClassWriter implements BerImplementationWriter {

  public BerGoLangStructWriter(HashMap<String, AsnModule> modulesByName,
      String outputBaseDir,
      String basePackageName, boolean disableWritingVersion) {

    super(modulesByName, outputBaseDir, basePackageName, false, disableWritingVersion, false);

    this.basePackageName.replace(".", "/"); // GoLang form
  }


}
