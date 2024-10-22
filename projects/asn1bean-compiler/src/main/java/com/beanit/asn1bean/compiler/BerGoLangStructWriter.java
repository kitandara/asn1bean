package com.beanit.asn1bean.compiler;

import com.beanit.asn1bean.ber.BerTag;
import com.beanit.asn1bean.compiler.model.*;
import com.beanit.asn1bean.util.HexString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BerGoLangStructWriter extends BerJavaClassWriter implements BerImplementationWriter {

  static final String[] LIB_SYMBOLS = new String[]{
      "BerAny",
      "BerBitString",
      "BerBmpString",
      "BerBoolean",
      "BerDate",
      "BerDateTime",
      "BerDuration",
      "BerEmbeddedPdv",
      "BerEnum",
      "BerGeneralizedTime",
      "BerGeneralString",
      "BerGraphicString",
      "BerIA5String",
      "BerInteger",
      "BerLength",
      "BerNull",
      "BerNumericString",
      "BerObjectDescriptor",
      "BerObjectIdentifier",
      "BerOctetString",
      "BerPrintableString",
      "BerReal",
      "BerTag",
      "Tagged",
      "BerTeletexString",
      "BerTime",
      "BerTimeOfDay",
      "BerType",
      "BerUniversalString",
      "BerUtcTime",
      "BerUTF8String",
      "BerVideotexString",
      "BerVisibleString",
      "ReversedIoWriter",
  };
  private static final String LIB_SRC = "github.com/kitandara/asn1ber";
  private static final String LIB_PREFIX = "asn1";
  private static final String GOLANG_VERSION = "1.23";
  List<String> outputFolders = new ArrayList<>();

  public BerGoLangStructWriter(HashMap<String, AsnModule> modulesByName,
      String outputBaseDir,
      String basePackageName, boolean disableWritingVersion) {
    super(modulesByName, outputBaseDir, basePackageName, false, disableWritingVersion, false);

    if (this.basePackageName.endsWith(".")) {
      this.basePackageName = this.basePackageName.substring(0, this.basePackageName.length() - 1); // Remove added '.'
    }
  }

  @Override
  protected boolean includeBasePackageNameInOutputDir() {
    return false;
  }

  @Override
  public void initModuleOutputDir(AsnModule module) {

    try {
      outputDirectory.mkdirs();
      // Generate module name...
      Writer fileWriter = Files.newBufferedWriter(new File(outputDirectory, "go.mod").toPath(), UTF_8);
      BufferedWriter bf = new BufferedWriter(fileWriter);
      String modName = moduleToPackageName(module.moduleIdentifier.name).replace('.', '/');
      outputFolders.add(modName);
      bf.write("module " + this.basePackageName + "/" + modName + "\n\n");
      bf.write("go " + GOLANG_VERSION + "\n");
      bf.close();
      fileWriter.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void postOutput() throws IOException {
    Writer fileWriter = Files.newBufferedWriter(new File(outputBaseDir, "go.work").toPath(), UTF_8);
    BufferedWriter bf = new BufferedWriter(fileWriter);
    bf.write("go " + GOLANG_VERSION + "\n");
    if (this.outputFolders.size() > 0) {
      bf.write("use (\n");
      for (String dir : this.outputFolders) {
        bf.write("\t\t./" + dir + "\n");
      }
      bf.write(")\n");
    }
    bf.close();
    fileWriter.close();

    initIntermediateDirs();
  }

  private void initIntermediateDirs() throws IOException {
    Map<String,Boolean> m = new HashMap<>();
    for (String d: this.outputFolders ) {
      int idx = d.lastIndexOf('/');
      String folder = idx>0 ? d.substring(0,idx) : d;
      m.put(folder,true);
    }
   Set<String> folders =  m.keySet();
    for (String f : folders) {
      initSubFolder(f);
    }
  }

  private void initSubFolder(String folder) throws IOException {

    String pkg = this.basePackageName +  "/" + folder;
    File dir = new File(this.outputBaseDir,folder);
    writeModFile(pkg,dir);

    int idx = folder.lastIndexOf('/');
    if (idx > 0) {
      String d = folder.substring(0,idx);
      if (d.equals("."))
        return;
      initSubFolder(d);
    }
  }

  private void writeModFile(String pkg, File baseDir) throws IOException
  {
    Writer fileWriter = Files.newBufferedWriter(new File(baseDir, "go.mod").toPath(), UTF_8);
    BufferedWriter bf = new BufferedWriter(fileWriter);
    bf.write("module " + pkg + "\n\n");
    bf.write("go " + GOLANG_VERSION + "\n");
    bf.close();
    fileWriter.close();
  }
  @Override
  public void initOutputDir() throws IOException {
    outputDirectory = new File(outputBaseDir, "."); // Force output dir to get created...
    outputDirectory.mkdirs();
    writeModFile(this.basePackageName, outputBaseDir);
  }

  @Override
  protected String moduleToPackageName(String moduleName) {
    return Utils.moduleToPackageName(moduleName, "/");
  }

  @Override
  protected void writeClassHeader(String typeName, AsnModule module) throws IOException {
    //noinspection ResultOfMethodCallIgnored
    outputDirectory.mkdirs();

    Writer fileWriter =
        Files.newBufferedWriter(new File(outputDirectory, typeName + ".go").toPath(), UTF_8);
    out = new BufferedWriter(fileWriter);

    String versionString = "";
    if (insertVersion) {
      versionString = " v" + Compiler.VERSION;
    }

    write("package " + Utils.lastPartOfPackageName(module.moduleIdentifier.name) + "\n");

    write("/*");
    write(
        " * This class file was automatically generated by ASN1bean"
            + versionString
            + " (http://www.beanit.com)\n */\n");

    write("import (\n"
        + "\t\"encoding/hex\"\n"
        + "\t\"errors\"\n"
        + "\t\"fmt\"\n"
        + "\t\"bytes\"\n"
        + "\t" + LIB_PREFIX + " \"" + LIB_SRC + "\"\n"
        + "\t\"io\"\n\n");

    // Import from others...
    List<String> importedClassesFromOtherModules = new ArrayList<>();

    for (SymbolsFromModule symbolsFromModule : module.importSymbolFromModuleList) {
      AsnModule importedModule = modulesByName.get(symbolsFromModule.modref);
      if (importedModule == null) {
        continue; // Might be ours..
      }
      for (String importedSymbol : symbolsFromModule.symbolList) {
        if (Character.isUpperCase(importedSymbol.charAt(0))) {
          if (importedModule.typesByName.get(importedSymbol) != null) {
            importedClassesFromOtherModules.add(
                moduleToPackageName(importedModule.moduleIdentifier.name)
            );
          }
        }
      }
    }
    Collections.sort(importedClassesFromOtherModules);
    for (String modulePackage : importedClassesFromOtherModules) {
      write("\t\"" + basePackageName + "/" + modulePackage + "\"");
    }
    write(")\n\n");
  }

  private String getModuleForType(String assignedTypeName) {

    if (module.typesByName.get(assignedTypeName) != null) {
      return null; // Internal to the module
    }
    for (SymbolsFromModule symbolsFromModule : module.importSymbolFromModuleList) {
      for (String sym : symbolsFromModule.symbolList) {
        if (sym.equalsIgnoreCase(assignedTypeName)) {
          return symbolsFromModule.modref;
        }
      }
    }
    return null;
  }

  @Override
  String getBerTagParametersString(BerImplementationWriter.Tag tag) {
    return LIB_PREFIX + "."
        + tag.tagClass
        + "_CLASS, " + LIB_PREFIX + "."
        + tag.typeStructure.toString()
        + ", "
        + tag.value;
  }

  @Override
  protected void writeRetaggingTypeClass(
      String typeName, String assignedTypeName, AsnType typeDefinition, BerImplementationWriter.Tag tag)
      throws IOException {

    // The embedded type.

    // Output the struct
    write("type " + typeName + " struct {");

    String xtype = normaliseClassName(assignedTypeName);
    write(xtype);

    write("}\n\n");

    String embeddedType = Utils.lastPartOfName(xtype);

    // Write tag func
    if (tag != null) {
      write("func (b *" + typeName + ") GetTag () {");
      write("\treturn " + LIB_PREFIX + ".NewBerTag(" + getBerTagParametersString(tag) + ")");
      write("}");

      // Ignore constructors, go straight to encoder/decode funcs
      write("func (b *" + typeName + ") Encode(reverseOS io.Writer, withTagList ...bool) (int, error) {");
      write("var withTag bool\n"
          + "\tif len(withTagList) > 0 {\n"
          + "\t\twithTag = withTagList[0]\n"
          + "\t} else {\n"
          + "\t\twithTag = true\n"
          + "\t}");
      write("codeLength := 0");
      write("var err error\n"
          + "\tvar n int");
      if (tag.type == BerImplementationWriter.TagType.EXPLICIT) {
        if (isDirectAnyOrChoice((AsnTaggedType) typeDefinition)) {
          write("n,err = b." + embeddedType + " .Encode(reverseOS)");
        } else {
          write("n,err = b." + embeddedType + ".Encode(reverseOS, true)");
        }
        writeErrorCheckerCode();

        write("codeLength += n");
        write("n,err = " + LIB_PREFIX + ".EncodeLength(codeLength, reverseOS)");
        write("codeLength += n");

      } else {
        write("n,err = b." + embeddedType + ".Encode(reverseOS, false)");
      }
      writeErrorCheckerCode();

      write("if withTag {");
      write("n,err = b.GetTag().Encode(reverseOS)");
      write("codeLength += n");
      writeErrorCheckerCode();
      write("}\n");

      write("return codeLength,nil");
      write("}\n");

      // .. now write Decode
      write("func (b *" + typeName + ") Decode(is io.Reader, withTagList ...bool) (int, error) {");
      write("var withTag bool\n"
          + "\tif len(withTagList) > 0 {\n"
          + "\t\twithTag = withTagList[0]\n"
          + "\t} else {\n"
          + "\t\twithTag = true\n"
          + "\t}\n"
          + "\tcodeLength := 0");
      write("var err error\n"
          + "\tvar n int");
      write("if withTag {");
      write("n,err = b.GetTag().decodeAndCheck(is);");
      write("codeLength += n");
      writeErrorCheckerCode();
      write("}\n");

      if (tag.type == BerImplementationWriter.TagType.EXPLICIT) {

        write(" length := &" + LIB_PREFIX + ".BerLength{}");
        write(" n,err = length.Decode(is)\n");
        write("codeLength += n");
        writeErrorCheckerCode();
        if (isDirectAnyOrChoice((AsnTaggedType) typeDefinition)) {
          write("n,err = b." + embeddedType + ".DecodeWithTag(is, null);");
        } else {
          write("n,err = b. " + embeddedType + ".Decode(is, true)");
        }
        write("codeLength += n");
        writeErrorCheckerCode();
        write("n,err = length.ReadEocIfIndefinite(is)\n");
      } else {
        write("n,err = b. " + embeddedType + ".Decode(is, false)");
      }
      write("codeLength += n");
      // writeErrorCheckerCode();

      write("return codeLength,err");
      write("}\n");
    }
    write("}");
  }

  private String normaliseClassName(String className) {
    String[] l = className.split("/");

    String xName;
    if (l.length > 1) {
      xName = l[l.length - 1];
    } else {
      String embeddedType = Utils.cleanUpName(className);

      String importModuleName = getModuleForType(className);
      xName = (importModuleName != null ? importModuleName + "." : "") + "" + embeddedType;
    }
    return xName;
  }

  @Override
  protected void writeMembers(List<AsnElementType> componentTypes) throws IOException {

    for (AsnElementType element : componentTypes) {
      String className = normaliseClassName(element.className);
      String elementName = Utils.cleanUpName(element.name);
      write(elementName + " *" + className);
    }
    write("");
  }

  @Override
  protected void writeChoiceClass(
      String className,
      AsnChoice asn1TypeElement,
      BerImplementationWriter.Tag tag,
      String isStaticStr,
      List<String> listOfSubClassNames)
      throws IOException {
    List<AsnElementType> componentTypes = asn1TypeElement.componentTypes;

    addAutomaticTagsIfNeeded(componentTypes);

    writeSubClasses(className, listOfSubClassNames, componentTypes);
    setClassNamesOfComponents(listOfSubClassNames, componentTypes, className);

    write("type " + className + " struct {\n");
    writeMembers(componentTypes);
    write("}\n");

    if (tag != null) {
      write("func (b *" + className + ") GetTag () {");
      write("\treturn " + LIB_PREFIX + ".NewBerTag(" + getBerTagParametersString(tag) + ")");
      write("}");
    }

    writeChoiceEncodeFunction(className, componentTypes, tag != null);

    writeChoiceDecodeMethod(className, convertToComponentInfos(componentTypes), tag != null);

    writeChoiceToStringFunction(className, componentTypes);
  }

  @Override
  protected void writeChoiceEncodeFunction(
      String className, List<AsnElementType> componentTypes, boolean hasExplicitTag) throws IOException {

    write("func (b *" + className + ") Encode(reverseOS io.Writer, withTagList ...bool) (int, error) {");
    if (!hasExplicitTag) {
      write("var withTag bool\n"
          + "\tif len(withTagList) > 0 {\n"
          + "\t\twithTag = withTagList[0]\n"
          + "\t} else {\n"
          + "\t\twithTag = true\n"
          + "\t}");
    }
    write("codeLength := 0");
    write("var err error\n"
        + "\tvar n int");
    for (int j = componentTypes.size() - 1; j >= 0; j--) {
      if (isExplicit(getTag(componentTypes.get(j)))) {
        write("var sublength int\n");
        break;
      }
    }

    for (int j = componentTypes.size() - 1; j >= 0; j--) {

      AsnElementType componentType = componentTypes.get(j);

      BerImplementationWriter.Tag componentTag = getTag(componentType);

      write("if b." + getVariableName(componentType) + " != nil {");

      String explicitEncoding = getExplicitEncodingParameter(componentType);

      if (isExplicit(componentTag)) {
        write(
            "sublenth,err = b."
                + getVariableName(componentType)
                + ".Encode(reverseOS"
                + explicitEncoding
                + ")");
        write("codeLength += sublength");
        write("n,err = " + LIB_PREFIX + ".EncodeLength( sublength,reverseOS)");
      } else {
        write(
            "n,err = b."
                + getVariableName(componentType)
                + ".Encode(reverseOS"
                + explicitEncoding
                + ");");
      }

      write("codeLength += n");
      writeErrorCheckerCode();

      if (componentTag != null) {
        writeEncodeTag(componentTag);
      }

      if (hasExplicitTag) {
        write("n,err = " + LIB_PREFIX + ".EncodeLength( codeLength,reverseOS)");
        write("codeLength += n");
        write("if withTag {");
        write("n,err = b.GetTag().encode(reverseOS)");
        write("codeLength += n");
        write("}");
        writeErrorCheckerCode();
      }

      write("return codeLength,nil");
      write("}");

      write("");
    }

    write("return 0, errors.New(\"error encoding CHOICE: No element of CHOICE was selected.\");");

    write("}\n");
  }

  @Override
  protected void writeSimpleDecodeFunction(String className, String param) throws IOException {
    write("func (b *" + className + ") Decode(is io.Reader, withTagList ...bool) (int, error) {");
    write("return b.DecodeWithTag(is, " + param + ")");
    write("}\n");
  }

  private void writeErrorCheckerCode() throws IOException {
    write("if err != nil {");
    write("return 0, err");
    write("}");
  }

  @Override
  protected void writeChoiceDecodeMethod(String className, List<ComponentInfo> components, boolean hasExplicitTag)
      throws IOException {

    if (hasExplicitTag) {
      write("func (b *" + className + ") Decode(is io.Reader, withTagList ...bool) (int, error) {");
      write("var withTag bool\n"
          + "\tif len(withTagList) > 0 {\n"
          + "\t\twithTag = withTagList[0]\n"
          + "\t} else {\n"
          + "\t\twithTag = true\n"
          + "\t}\n"
          + "");
      write("var err error\n"
          + "\tvar n int");

      write(" tlvByteCount := 0");
      write("berTag := new (" + LIB_PREFIX + ".BerTag)\n");

      write("if withTag {");
      write("n,err = b.GetTag().DecodeAndCheck(is)");
      write("tlvByteCount += n");
      writeErrorCheckerCode();

      write("}\n");
      write(" explicitTagLength := &" + LIB_PREFIX + ".BerLength{}");

      write("n,err = explicitTagLength.Decode(is)");
      write("tlvByteCount += n");
      write("n,err = berTag.Decode(is)\n");
      write("tlvByteCount += n");
      writeErrorCheckerCode();
    } else {

      writeSimpleDecodeFunction(className, "null");

      write("func (b *" + className + ") DecodeWithTag(is io.Reader, berTag *" + LIB_PREFIX + ".BerTag) (int, error) {");
      write("var err error\n"
          + "\tvar n int");

      write("tlvByteCount := 0");
      write("tagWasPassed := (berTag != nil)\n");

      write("if berTag == nil {");
      write("berTag = new(" + LIB_PREFIX + ".BerTag)");
      write("n,err = berTag.Decode(is)");
      write("tlvByteCount += n");
      writeErrorCheckerCode();
      write("}\n");
    }

    if (containsUntaggedChoiceOrAny(components)) {
      write("var numDecodedBytes int\n");
    }

    for (ComponentInfo component : components) {
      if (component.isDirectChoiceOrAny && (component.tag == null)) {
        writeChoiceComponentDecodeUntaggedChoiceOrAny(component, hasExplicitTag);
      } else {
        writeChoiceComponentDecodeRegular(className, component, hasExplicitTag);
      }
    }

    if (!hasExplicitTag) {
      write("if tagWasPassed {");
      write("return 0,nil");
      write("}\n");
    }

    write(
        "return errors.New(fmt.Sprintf(\"Error decoding CHOICE: Tag %s matched to no item.\",berTag))");

    write("}\n");
  }

  @Override
  protected void writeChoiceComponentDecodeRegular(String className, ComponentInfo component, boolean taggedChoice)
      throws IOException {
    if (component.tag != null) {
      write("if berTag.Equals(" + getBerTagParametersString(component.tag) + ") {");
    } else {
      write("if berTag.EqualsTag(new(" + normaliseClassName(component.className) + ").GetTag()) {");
    }

    if (isExplicit(component.tag)) {
      write(" length := &" + LIB_PREFIX + ".BerLength{}");
      write(" n,err = length.Decode(is)\n");
      write("tlvByteCount += n");
      writeErrorCheckerCode();
    }

    write("b." + component.variableName + " = new (" + normaliseClassName(component.className) + ")");
    write(
        "n,err = "
            + component.variableName
            + ".Decode" + getDecodeFuncSuffix(component) + "(is, "
            + getDecodeTagParameter(component)
            + ")");
    write("tlvByteCount += n");
    if (isExplicit(component.tag)) {
      write("n,err = length.ReadEocIfIndefinite(is)");
      write("tlvByteCount += n");
      writeErrorCheckerCode();
    }
    if (taggedChoice) {
      write("n,err = explicitTagLength.ReadEocIfIndefinite(is)");
      write("tlvByteCount += n");
      writeErrorCheckerCode();
    }
    write("return tlvByteCount,nil");
    write("}\n");
  }

  @Override
  protected void writeChoiceComponentDecodeUntaggedChoiceOrAny(
      ComponentInfo component, boolean taggedChoice) throws IOException {
    write("b." + component.variableName + " = new (" + normaliseClassName(component.className) + ")");
    write(
        "numDecodedBytes,err = b."
            + component.variableName
            + ".Decode" + getDecodeFuncSuffix(component) + "(is, "
            + getDecodeTagParameter(component)
            + ")");

    write("if numDecodedBytes != 0 {");
    if (taggedChoice) {
      write("n,err = explicitTagLength.ReadEocIfIndefinite(is);");
      write("tlvByteCount += n");
    }
    write("return tlvByteCount + numDecodedBytes,nil");
    write("} else {");
    write("b." + component.variableName + " = nil");
    write("}\n");
  }

  @Override
  protected void writeSequenceOrSetClass(
      String className,
      AsnSequenceSet asnSequenceSet,
      BerImplementationWriter.Tag tag,
      String isStaticStr,
      List<String> listOfSubClassNames)
      throws IOException {

    List<AsnElementType> componentTypes = asnSequenceSet.componentTypes;
    addAutomaticTagsIfNeeded(componentTypes);
    setClassNamesOfComponents(listOfSubClassNames, componentTypes, className);
    writeSubClasses(className, listOfSubClassNames, componentTypes);

    write("type " + className + " struct {");
    writeMembers(componentTypes);
    write("}\n\n");

    BerImplementationWriter.Tag mainTag = tagFromSequenceSet(tag, asnSequenceSet.isSequence);

    // Write tag func.
    write("func (b *" + className + ") GetTag () {");
    write("\treturn " + LIB_PREFIX + ".NewBerTag(" + getBerTagParametersString(mainTag) + ")");
    write("}");
    boolean hasExplicitTag = (tag != null) && (tag.type == BerImplementationWriter.TagType.EXPLICIT);

    writeSequenceOrSetEncodeFunction(className, componentTypes, hasExplicitTag, asnSequenceSet.isSequence);
    if (asnSequenceSet.isSequence) {
      writeSequenceDecodeMethod(className, convertToComponentInfos(componentTypes), hasExplicitTag);
    } else {
      writeSetDecodeFunction(className, convertToComponentInfos(componentTypes), hasExplicitTag);
    }
    writeSequenceOrSetToStringFunction(className, componentTypes);
  }

  @Override
  protected void writeSetComponentDecodeRegular(ComponentInfo component, boolean first)
      throws IOException {

    String elseString = first ? "" : " else ";
    if (component.tag != null) {
      write(elseString + "if berTag.Equals(" + getBerTagParametersString(component.tag) + ") {");
    } else {
      write(elseString + "if (berTag.Equals(new(" + normaliseClassName(component.className) + ").GetTag())) {");
    }

    if (isExplicit(component.tag)) {
      write("n,err = length.Decode(is)");
      write("vByteCount += n");
      writeErrorCheckerCode();

    }

    write("b." + component.variableName + " = new (" + normaliseClassName(component.className) + ")");
    write(
        "n,err = b."
            + component.variableName
            + ".Decode" + getDecodeFuncSuffix(component) + "(is, "
            + getDecodeTagParameter(component)
            + ")");
    write("vByteCount += n");
    writeErrorCheckerCode();

    if (isExplicit(component.tag)) {
      write("n,err = length.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
      writeErrorCheckerCode();
    }
    out.write("\n\t} "); // The 'else' in golang must be on same line...
  }

  @Override
  protected void writeSetDecodeFunction(String className, List<ComponentInfo> components, boolean hasExplicitTag)
      throws IOException {

    write("func (b *" + className + ") Decode(is io.Reader, withTagList ...bool) (int, error) {");
    write("var withTag bool\n"
        + "\tif len(withTagList) > 0 {\n"
        + "\t\twithTag = withTagList[0]\n"
        + "\t} else {\n"
        + "\t\twithTag = true\n"
        + "\t}\n");
    write("var err error\n"
        + "\tvar n int");
    write("tlByteCount := 0");
    write("vByteCount := 0");
    write("berTag := new(" + LIB_PREFIX + ".BerTag)\n");

    write("if withTag {");
    write("n,err = b.GetTag().decodeAndCheck(is)");
    write("tlByteCount += n");
    writeErrorCheckerCode();

    write("}\n");

    if (hasExplicitTag) {
      write("explicitTagLength := &" + LIB_PREFIX + ".BerLength{}");
      write("n,err = explicitTagLength.Decode(is)");
      write("tlByteCount += n");
      write("n,err = " + LIB_PREFIX + ".SET.DecodeAndCheck(is)\n");
      write("tlByteCount += n");
      writeErrorCheckerCode();
    }

    write("length := &" + LIB_PREFIX + ".BerLength{}");
    write("n,err = length.Decode(is)");
    write("tlByteCount += n");
    writeErrorCheckerCode();
    write("lengthVal = length.Length\n");

    if (allOptionalOrDefault(components)) {
      write("if lengthVal == 0 {");
      write("return tlByteCount,nil");
      write("}\n");
    }

    write("for (vByteCount < lengthVal || lengthVal < 0) {");
    write("n,err = berTag.Decode(is);");
    write("vByteCount += n");

    boolean first = true;
    for (ComponentInfo component : components) {
      if (component.isDirectChoiceOrAny && (component.tag == null)) {
        throw new IOException("choice or ANY within set has no explicit tag.");
      } else {
        writeSetComponentDecodeRegular(component, first);
      }
      first = false;
    }

    write("else if lengthVal < 0 && berTag.Equals(0, 0, 0)) {");
    write("err = " + LIB_PREFIX + ". ReadEocByte(is)");
    write("vByteCount += 1");
    writeErrorCheckerCode();

    if (hasExplicitTag) {
      write("n,err = explicitTagLength.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
    }
    write("return tlByteCount + vByteCount,err");
    write("} else {");
    write("return 0,  errors.New(\"tag does not match any set component: \" + berTag)");
    write("}");

    write("}");

    write("if (vByteCount != lengthVal) {");
    write(
        "return 0,errors.New  (fmt.Sprintf(\"Length of set does not match length tag, length tag: %d, actual set length: %d \",  lengthVal, vByteCount))");
    write("}");
    if (hasExplicitTag) {
      write("n,err = explicitTagLength.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
    }

    write("return tlByteCount + vByteCount,err");
    write("}\n");
  }

  @Override
  protected void writeEncodeTag(BerImplementationWriter.Tag tag) throws IOException {
    int typeStructure;

    if (tag.typeStructure == BerImplementationWriter.TypeStructure.CONSTRUCTED) {
      typeStructure = BerTag.CONSTRUCTED;
    } else {
      typeStructure = BerTag.PRIMITIVE;
    }

    BerTag berTag = new BerTag(getTagClassId(tag.tagClass.toString()), typeStructure, tag.value);

    write("// write tag: " + tag.tagClass + "_CLASS, " + tag.typeStructure + ", " + tag.value);
    for (int i = (berTag.tagBytes.length - 1); i >= 0; i--) {
      write("_,_ = " + LIB_PREFIX + ".WriteByte(reverseOS, 0x" + HexString.fromByte(berTag.tagBytes[i]) + ")");
    }

    write("codeLength += " + berTag.tagBytes.length + "");
  }

  @Override
  protected String getClassNameOfSequenceOfElement(
      AsnElementType componentType, List<String> listOfSubClassNames) {
    String classNameOfSequenceElement = getClassNameOfSequenceOfElement(componentType);
    for (String subClassName : listOfSubClassNames) {
      if (classNameOfSequenceElement.equals(subClassName)) {
        String moduleName = module.moduleIdentifier.name;

        for (SymbolsFromModule symbols : this.module.importSymbolFromModuleList) {
          if (symbols.symbolList.contains(classNameOfSequenceElement)) {
            moduleName = symbols.modref;
            break;
          }
        }

        return Utils.lastPartOfPackageName(moduleToPackageName(moduleName)) + "." + classNameOfSequenceElement;
      }
    }
    return classNameOfSequenceElement;
  }

  @Override
  protected void writeSequenceOrSetEncodeFunction(
      String typeName, List<AsnElementType> componentTypes, boolean hasExplicitTag, boolean isSequence)
      throws IOException {
    write("func (b *" + typeName + ") Encode(reverseOS io.Writer, withTagList ...bool) (int, error) {");

    write("var withTag bool\n"
        + "\tif len(withTagList) > 0 {\n"
        + "\t\twithTag = withTagList[0]\n"
        + "\t} else {\n"
        + "\t\twithTag = true\n"
        + "\t}");

    write("codeLength := 0");
    write("var err error\n"
        + "\tvar n int");
    for (int j = componentTypes.size() - 1; j >= 0; j--) {
      if (isExplicit(getTag(componentTypes.get(j)))) {
        write("var sublength int\n");
        break;
      }
    }

    for (int j = componentTypes.size() - 1; j >= 0; j--) {

      AsnElementType componentType = componentTypes.get(j);

      BerImplementationWriter.Tag componentTag = getTag(componentType);

      if (isOptional(componentType)) {
        write("if b." + getVariableName(componentType) + " != nil {");
      } else {
        write("if b." + getVariableName(componentType) + " == nil {");
        write("return 0,errors.New(\"Missing component: " + getVariableName(componentType) + "\") ");
        write("}");
      }

      String explicitEncoding = getExplicitEncodingParameter(componentType);

      if (isExplicit(componentTag)) {
        write(
            "n,err = b."
                + getVariableName(componentType)
                + ".Encode(reverseOS"
                + explicitEncoding
                + ")");
        write("sublength += n");
        write("codeLength += sublength");
        writeErrorCheckerCode();
        write("n,err = " + LIB_PREFIX + ".EncodeLength(sublength,reversOS)");
      } else {
        write(
            "n,err = b."
                + getVariableName(componentType)
                + ".encode(reverseOS"
                + explicitEncoding
                + ");");
      }
      write("codeLength += n");
      writeErrorCheckerCode();

      if (componentTag != null) {
        writeEncodeTag(componentTag);
      }
      if (isOptional(componentType)) {
        write("}");
      }

      write("");
    }

    if (hasExplicitTag) {
      write("n,err = " + LIB_PREFIX + ".EncodeLength(codeLength,reverseOS)");
      write("codeLength += n");
      if (isSequence) {
        write("_,_  = " + LIB_PREFIX + ".WriteByte(reverseOS,0x30)");
      } else {
        write("_,_  = " + LIB_PREFIX + ".WriteByte(reverseOS,0x31)");
      }
      write("codeLength++\n");
    }

    write("n,err = " + LIB_PREFIX + ".EncodeLength(codeLength,reverseOS)\n");
    write("codeLength += n");
    write("if withTag {");
    write("n,err = b.GetTag().Encode(reverseOS)");
    write("codeLength += n");
    write("}\n");

    write("return codeLength,err\n");

    write("}\n");
  }

  @Override
  protected void writeSequenceOfClass(
      String className,
      AsnSequenceOf asnSequenceOf,
      BerImplementationWriter.Tag tag,
      String isStaticStr,
      List<String> listOfSubClassNames)
      throws IOException {

    AsnElementType componentType = asnSequenceOf.componentType;

    String referencedTypeName = getClassNameOfSequenceOfElement(componentType, listOfSubClassNames);

    if (isInnerType(componentType)) {
      module.subClassCount++;
      writeConstructedTypeClass(
          referencedTypeName, componentType.typeReference, null, true, listOfSubClassNames);
      module.subClassCount--;
    }

    BerImplementationWriter.Tag mainTag = tagFromSequenceSet(tag, asnSequenceOf.isSequenceOf);

    write("type " + className + " struct {");
    write("SeqOf []*" + normaliseClassName(referencedTypeName));
    write("}\n\n");

    // Write tag func.
    write("func (b *" + className + ") GetTag () {");
    write("\treturn " + LIB_PREFIX + ".NewBerTag(" + getBerTagParametersString(mainTag) + ")");
    write("}");

    boolean hasExplicitTag = (tag != null) && (tag.type == BerImplementationWriter.TagType.EXPLICIT);

    writeSequenceOfEncodeFunction(className, componentType, hasExplicitTag, asnSequenceOf.isSequenceOf);

    writeSequenceOrSetOfDecodeFunction(className,
        convertToComponentInfo(componentType, false, referencedTypeName),
        hasExplicitTag,
        asnSequenceOf.isSequenceOf);

    writeSequenceOrSetOfToStringFunction(className, referencedTypeName, componentType);
  }

  @Override
  protected void writeSequenceOfEncodeFunction(
      String className, AsnElementType componentType, boolean hasExplicitTag, boolean isSequence) throws IOException {

    write("func (b *" + className + ") Encode(reverseOS io.Writer, withTagList ...bool) (int, error) {");

    write("var withTag bool\n"
        + "\tif len(withTagList) > 0 {\n"
        + "\t\twithTag = withTagList[0]\n"
        + "\t} else {\n"
        + "\t\twithTag = true\n"
        + "\t}\n"
        + "\tcodeLength := 0");
    write("var err error\n"
        + "\tvar n int");

    write("for  i = len(b.SeqOf) - 1; i >= 0; i-- {");

    BerImplementationWriter.Tag componentTag = getTag(componentType);
    String explicitEncoding = getExplicitEncodingParameter(componentType);

    if (componentTag != null) {

      if (componentTag.type == BerImplementationWriter.TagType.EXPLICIT) {
        write("sublength,err = b.SeqOf[i].Encode(reverseOS" + explicitEncoding + ")");
        write("codeLength += sublength");
        write("n,err = " + LIB_PREFIX + ".EncodeLength( sublength, reverseOS)");

      } else {
        write("n,err = b.SeqOf[i].Encode(reverseOS" + explicitEncoding + ")");
      }
      write("codeLength += n");
      writeErrorCheckerCode();
      writeEncodeTag(componentTag);
    } else {

      if (isDirectAnyOrChoice(componentType)) {
        write("n,err = b.SeqOf[i].Encode(reverseOS)");
      } else {
        write("n,err = b.SeqOf[i].Encode(reverseOS, true)");
      }
      write("codeLength += n");
      writeErrorCheckerCode();
    }

    write("}\n");

    if (hasExplicitTag) {
      write("n,err = " + LIB_PREFIX + ".EncodeLength( codeLength,reverseOS)");
      if (isSequence) {
        write("_,_ = " + LIB_PREFIX + ".WriteByte(reverseOS, 0x30)");
      } else {
        write("_,_ = " + LIB_PREFIX + ".WriteByte(reverseOS, 0x31)");
      }
      writeErrorCheckerCode();
      write("codeLength++;\n");
    }

    write("n,err = " + LIB_PREFIX + ".EncodeLength(codeLength,reverseOS)\n");
    write("codeLength += n");
    write("if withTag {");
    write("n,err= b.GetTag().Encode(reverseOS)");
    write("codeLength += n");
    write("}\n");

    write("return codeLength,nil");
    write("}\n");
  }

  @Override
  protected void writeSequenceOrSetOfDecodeFunction(
      String className, ComponentInfo component, boolean hasExplicitTag, boolean isSequence) throws IOException {

    write("func (b *" + className + ") Decode(is io.Reader, withTagList ...bool) (int, error) {");
    write("var withTag bool\n"
        + "\tif len(withTagList) > 0 {\n"
        + "\t\twithTag = withTagList[0]\n"
        + "\t} else {\n"
        + "\t\twithTag = true\n"
        + "\t}\n");
    write("var err error\n"
        + "\tvar n int");

    write("tlByteCount := 0");
    write("vByteCount := 0");
    if (containsUntaggedChoiceOrAny(Collections.singletonList(component))) {
      write("var numDecodedBytes int");
    }
    write("berTag := new (" + LIB_PREFIX + ".BerTag)");

    write("if withTag {");
    write("n,err = b.GetTag().DecodeAndCheck(is)");
    write("tlByteCount += n");
    writeErrorCheckerCode();
    write("}\n");

    if (hasExplicitTag) {
      write("explicitTagLength := &" + LIB_PREFIX + ".BerLength{}");
      write("n,err = explicitTagLength.Decode(is)");
      write("tlByteCount += n");
      writeErrorCheckerCode();
      if (isSequence) {
        write("n,err = " + LIB_PREFIX + ".SEQUENCE.DecodeAndCheck(is)\n");
      } else {
        write("n,err = " + LIB_PREFIX + ".SET.DecodeAndCheck(is)\n");
      }
      write("tlByteCount += n");
      writeErrorCheckerCode();
    }

    write("length := &" + LIB_PREFIX + ".BerLength{}");
    write("n,err = length.Decode(is)");
    write("tlByteCount += n");
    write("lengthVal := length.Length;\n");
    writeErrorCheckerCode();
    write("for (vByteCount < lengthVal || lengthVal < 0) {");
    write("n,err = berTag.Decode(is)\n");
    write("vByteCount += n");
    write("if lengthVal < 0 && berTag.Equals(0, 0, 0) {");
    write("err = " + LIB_PREFIX + ".ReadEocByte(is)");
    write("vByteCount += 1");
    write("break;");
    write("}\n");

    if (component.isDirectChoiceOrAny && (component.tag == null)) {
      writeSequenceOfComponentDecodeUntaggedChoiceOrAny(component);
    } else {
      writeSequenceOfComponentDecodeRegular(component);
    }

    write("}");

    write("if lengthVal >= 0 && vByteCount != lengthVal {");
    write(
        "return 0, errors.New(fmt.Sprintf(\"Decoded SequenceOf or SetOf has wrong length. Expected %d but has %d \", lengthVal, vByteCount ))\n");
    write("}");

    if (hasExplicitTag) {
      write("n,err = explicitTagLength.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
    }
    write("return tlByteCount + vByteCount,err");
    write("}\n");
  }

  @Override
  protected void writeSequenceOfComponentDecodeUntaggedChoiceOrAny(ComponentInfo component)
      throws IOException {
    write("element := new(" + normaliseClassName(component.className) + ")");
    write("numDecodedBytes,err = " + "element.Decode" + getDecodeFuncSuffix(component) + "(is, " + getDecodeTagParameter(component) + ")");
    write("if (numDecodedBytes == 0) {");
    write("return 0,errors.New(\"tag did not match\")");
    write("}");
    write("vByteCount += numDecodedBytes");
    write("b.SeqOf = append(b.sqIf,element)");
  }

  @Override
  protected void writeSequenceOfComponentDecodeRegular(ComponentInfo component) throws IOException {
    if (component.tag != null) {
      write("if !berTag.Equals(" + getBerTagParametersString(component.tag) + ") {");
    } else {
      write("if (!berTag.EqualsTag(new(" + normaliseClassName(component.className) + ").GetTag()) {");
    }
    write("return 0, errors.New(\"tag does not match mandatory sequence of/set of component.\")");
    write("}");

    if (isExplicit(component.tag)) {
      write("n,err = length.Decode(is)");
      write("vByteCount += n");
    }
    write("element := new (" + normaliseClassName(component.className) + ")");
    write("n,err = " + "element.Decode" + getDecodeFuncSuffix(component) + "(is, " + getDecodeTagParameter(component) + ")");
    write("vByteCount += n");
    write("b.SeqOf = append(b.SeqOf,element)");
    writeErrorCheckerCode();
    if (isExplicit(component.tag)) {
      write("n,err = length.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
      writeErrorCheckerCode();
    }
  }

  @Override
  protected void writeSequenceDecodeMethod(String className, List<ComponentInfo> components, boolean hasExplicitTag)
      throws IOException {
    write("func (b *" + className + ") Decode(is io.Reader, withTagList ...bool) (int, error) {");

    write("var withTag bool\n"
        + "\tif len(withTagList) > 0 {\n"
        + "\t\twithTag = withTagList[0]\n"
        + "\t} else {\n"
        + "\t\twithTag = true\n"
        + "\t}\n");
    write("var err error\n"
        + "\tvar n int");

    write(" tlByteCount := 0");
    write(" vByteCount := 0");
    if (containsUntaggedChoiceOrAny(components)) {
      write("var numDecodedBytes int");
    }
    write("berTag := new(" + LIB_PREFIX + ".BerTag)\n");

    write("if withTag {");
    write("n,err = b.GetTag().decodeAndCheck(is);");
    write("tlByteCount += n");
    writeErrorCheckerCode();
    write("}\n");

    if (hasExplicitTag) {
      write("explicitTagLength := &" + LIB_PREFIX + ". BerLength{}");
      write("n,err = explicitTagLength.Decode(is)");
      write("tlByteCount += n");
      write("n,err =  " + LIB_PREFIX + ".SEQUENCE.DecodeAndCheck(is)\n");
      write("tlByteCount += n");
      writeErrorCheckerCode();
    }

    write(" length := &" + LIB_PREFIX + ". BerLength{}");
    write("n,err = length.Decode(is)");
    write("tlByteCount += n");
    write("lengthVal := length.Length");

    if (allOptionalOrDefault(components)) {
      write("if lengthVal == 0 {");
      write("return tlByteCount,nil");
      write("}");
    }
    write("n,err = berTag.Decode(is)\n");
    write("vByteCount += n");
    writeErrorCheckerCode();

    for (ComponentInfo component : components) {
      if (component.isDirectChoiceOrAny && (component.tag == null)) {
        writeSequenceComponentDecodeUntaggedChoiceOrAny(component);
      } else {
        writeSequenceComponentDecodeRegular(component);
        write("");
      }
    }

    if (extensibilityImplied) {
      writeSequenceDecodeMethodExtensibleEnd(hasExplicitTag);
    } else {
      writeSequenceDecodeMethodNonExtensibleEnd(hasExplicitTag);
    }

    write("}\n");
  }

  @Override
  protected void writeSequenceDecodeMethodExtensibleEnd(boolean hasExplicitTag) throws IOException {
    write("if lengthVal < 0 {");
    write("for (!berTag.Equals(0, 0, 0)) {");
    write("n,err = " + LIB_PREFIX + ". DecodeUnknownComponent(is)");
    writeErrorCheckerCode();
    write("vByteCount += n");
    write("n,err = berTag.Decode(is)");
    write("vByteCount += n");
    write("}");

    write("_ =  " + LIB_PREFIX + ".  ReadEocByte(is)");
    write("vByteCount += 1");
    if (hasExplicitTag) {
      write("n,err = explicitTagLength.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
      writeErrorCheckerCode();
    }
    write("return tlByteCount + vByteCount,nil");
    write("} else {");
    write("for (vByteCount < lengthVal) {");
    write("n,err = " + LIB_PREFIX + ". DecodeUnknownComponent(is)");
    write("vByteCount += n");
    writeErrorCheckerCode();
    write("if vByteCount == lengthVal {");
    write("return tlByteCount + vByteCount,nil");
    write("}");
    write("n,err = berTag.decode(is)");
    write("vByteCount += n");
    writeErrorCheckerCode();
    write("}");
    write("}");
    write(
        "return 0,errors.New(fmt.Sprintf(\"unexpected end of sequence, length tag: %d, bytes decoded: %d\", lengthVal, vByteCount))");
  }

  private String getDecodeFuncSuffix(ComponentInfo component) {
    if (component.isDirectChoiceOrAny) {
      return "WithTag";
    }
    return "";
  }

  @Override
  protected void writeSequenceComponentDecodeUntaggedChoiceOrAny(ComponentInfo component)
      throws IOException {
    write("b." + component.variableName + " = new (" + normaliseClassName(component.className) + ")");
    write(
        "numDecodedBytes,_ = b."
            + component.variableName
            + ".Decode" + getDecodeFuncSuffix(component) + " (is, "
            + getDecodeTagParameter(component)
            + ")");

    write("if numDecodedBytes != 0 {");
    write("vByteCount += numDecodedBytes");

    if (component.mayBeLast) {
      writeReturnIfDefiniteLengthMatchesDecodedBytes();
    }
    write("n,err = berTag.Decode(is)");
    write("vByteCount += n");
    writeErrorCheckerCode();
    write("}");
    if (component.isOptionalOrDefault) {
      write("else {");
      write("b." + component.variableName + " = nil");
      write("}");
    } else {
      writeElseThrowTagMatchingException();
    }
  }

  @Override
  protected void writeSequenceComponentDecodeRegular(ComponentInfo component) throws IOException {

    if (component.tag != null) {
      write("if (berTag.Equals(" + getBerTagParametersString(component.tag) + ")) {");
    } else {
      write("if (berTag.EqualsTag(new(" + normaliseClassName(component.className) + ").GetTag())) {");
    }

    if (isExplicit(component.tag)) {
      write("n,_ = length.Decode(is)");
      write("vByteCount += n");
    }

    write("b." + component.variableName + " = new (" + normaliseClassName(component.className) + ");");
    write(
        "n,err = b."
            + component.variableName
            + ".Decode " + getDecodeFuncSuffix(component) + "(is, "
            + getDecodeTagParameter(component)
            + ")");
    write("vByteCount += n");
    writeErrorCheckerCode();
    if (isExplicit(component.tag)) {
      write("n,err = length.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
      writeErrorCheckerCode();
    }

    if (component.mayBeLast) {
      writeReturnIfDefiniteLengthMatchesDecodedBytes();
    }
    write("n,err = berTag.decode(is);");
    write("vByteCount += n");
    writeErrorCheckerCode();
    write("}");
    if (!component.isOptionalOrDefault) {
      writeElseThrowTagMatchingException();
    }
  }

  @Override
  protected void writeSequenceDecodeMethodNonExtensibleEnd(boolean hasExplicitTag)
      throws IOException {
    write("if lengthVal < 0 {");
    write("if !berTag.Equals(0, 0, 0) {");
    write("return 0,errors.New(\"decoded sequence has wrong end of contents octets\")");
    write("}");
    write("n,err =  " + LIB_PREFIX + ".ReadEocByte(is)");
    write("vByteCount += n");
    writeErrorCheckerCode();
    if (hasExplicitTag) {
      write("n,err = explicitTagLength.ReadEocIfIndefinite(is)");
      write("vByteCount += n");
    }
    write("return tlByteCount + vByteCount,err");
    write("}\n");

    write(
        "return 0,errors.New(fmt.Sprintf(\"unexpected end of sequence, length tag: %d , bytes decoded: %d\",  lengthVal , vByteCount))\n");
  }

  @Override
  protected void writeElseThrowTagMatchingException() throws IOException {
    write("else {");
    write(" return 0,errors.New(\"tag does not match mandatory sequence component.\")");
    write("}");
  }

  @Override
  protected void writeReturnIfDefiniteLengthMatchesDecodedBytes() throws IOException {
    write("if lengthVal >= 0 && vByteCount == lengthVal {");
    write("return tlByteCount + vByteCount,nil");
    write("}");
  }

  @Override

  protected void writeToStringFunction(String className) throws IOException {
    write("func(b * " + className + ") S() string {");
    write("var sb bytes.Buffer");
    write("b.AppendAsString(sb, 0)");
    write("return sb.String()");
    write("}\n");
  }

  @Override
  protected void writeChoiceToStringFunction(String className, List<AsnElementType> componentTypes) throws IOException {
    writeToStringFunction(className);

    write("func (b * " + className + ")  AppendAsString( sb bytes.Buffer,   indentLevel int) {\n");

    for (AsnElementType componentType : componentTypes) {
      write("if b." + getVariableName(componentType) + " != nil {");

      if (!isPrimitive(getUniversalType(componentType))) {
        write("sb.WriteString(\"" + getVariableName(componentType) + ": \")");
        write("b." + getVariableName(componentType) + ".AppendAsString(sb, indentLevel + 1)");
      } else {
        write(
            "sb.WriteString(\""
                + getVariableName(componentType)
                + ": \")\n\tsb.WriteString(b."
                + getVariableName(componentType)
                + ".S());");
      }
      write("}\n");
    }

    write("sb.WriteString(\"<none>\");");

    write("}\n");
  }

  @Override
  protected void writeSequenceOrSetToStringFunction(String className, List<AsnElementType> componentTypes)
      throws IOException {

    writeToStringFunction(className);
    write("func (b * " + className + ")  AppendAsString( sb bytes.Buffer,   indentLevel int) {\n");

    write("sb.WriteString(\"{\");");

    boolean checkIfFirstSelectedElement = componentTypes.size() > 1;

    int j = 0;

    for (AsnElementType componentType : componentTypes) {

      if (isOptional(componentType)) {
        if (j == 0 && componentTypes.size() > 1) {
          write(" firstSelectedElement := true");
        }
        write("if b." + getVariableName(componentType) + " != nil {");
      } else {
        write("if b." + getVariableName(componentType) + " == nil {");
        write("return 0,errors.New(\"Missing component: " + getVariableName(componentType) + "\" ");
        write("}");
      }

      if (j != 0) {
        if (checkIfFirstSelectedElement) {

          write("if !firstSelectedElement {");
        }
        write("sb.WriteString(\",\\n\")");
        if (checkIfFirstSelectedElement) {
          write("}");
        }
      } else {
        write("sb.WriteString(\"\\n\")");
      }

      write("for  i := 0; i < indentLevel + 1; i++ {");
      write("sb.WriteString(\"\\t\");");
      write("}");
      if (!isOptional(componentType)) {
        write("if b." + getVariableName(componentType) + " != nil {");
      }
      if (!isPrimitive(getUniversalType(componentType))) {
        write("sb.WriteString(\"" + getVariableName(componentType) + ": \");");
        write("b." + getVariableName(componentType) + ".AppendAsString(sb, indentLevel + 1)");
      } else {
        write(
            "sb.WriteString(\""
                + getVariableName(componentType)
                + ": \")");
        write(" sb.WriteString(b."
            + getVariableName(componentType)
            + ".S())");
      }
      if (!isOptional(componentType)) {
        write("} else {");
        write("sb.WriteString(\"" + getVariableName(componentType) + ": <empty-required-field>\")");
        write("}");
      }

      if (isOptional(componentType)) {
        if (checkIfFirstSelectedElement && j != (componentTypes.size() - 1)) {
          write("firstSelectedElement := false");
        }
        write("}");
      } else {
        checkIfFirstSelectedElement = false;
      }

      write("");

      j++;
    }

    write("sb.WriteString(\"\\n\")");
    write("for i := 0; i < indentLevel; i++ {");
    write("sb.WriteString(\"\\t\")");
    write("}");
    write("sb.WriteString(\"}\")");

    write("}\n");
  }

  @Override
  protected void writeSequenceOrSetOfToStringFunction(
      String className, String referencedTypeName, AsnElementType componentType) throws IOException {

    writeToStringFunction(className);

    write("func (b * " + className + ")  AppendAsString( sb bytes.Buffer,   indentLevel int) {\n");

    write("sb.WriteString(\"{\\n\");");
    write("for  i := 0; i < indentLevel + 1; i++ {");
    write("sb.WriteString(\"\\t\");");
    write("}");

    write("if b.SeqOf == nil {");
    write("sb.WriteString(\"null\");");
    write("} else {");

    write("for _, element : range b.SeqOf  {");

    if (!isPrimitive(getUniversalType(componentType))) {
      write("element.AppendAsString(sb, indentLevel + 1)");
    } else {
      write("sb.WriteString(element.S())");
    }
    write("sb.WriteString(\",\\n\");");
    write("for  i := 0; i < indentLevel + 1; i++ {");
    write("sb.WriteString(\"\\t\");");
    write("}");

    write("} ");

    write("}\n");

    write("sb.WriteString(\"\\n\");");
    write("for  i := 0; i < indentLevel; i++ {");
    write("sb.WriteString(\"\\t\");");
    write("}");
    write("sb.WriteString(\"}\");");

    write("}\n");
  }

  @Override
  public void addLibSymbols(AsnModule module) {
    // Add defaults
    SymbolsFromModule e = new SymbolsFromModule();
    e.modref = LIB_PREFIX;
    e.symbolList = Arrays.asList(LIB_SYMBOLS);

    module.importSymbolFromModuleList.add(e);
  }
}