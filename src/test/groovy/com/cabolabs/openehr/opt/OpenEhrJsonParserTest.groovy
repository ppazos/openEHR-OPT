package  com.cabolabs.openehr.opt

import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer

import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.AdminEntry
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId
import groovy.util.GroovyTestCase
import groovy.json.JsonOutput
import com.cabolabs.openehr.rm_1_0_2.common.change_control.OriginalVersion

import com.networknt.schema.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation

class OpenEhrJsonParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   void testJsonParserInstruction()
   {
      String path = PS +"canonical_json"+ PS +"lab_order.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserObservation()
   {
      String path = PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserReferralWithParticipations()
   {
      String path = PS +"canonical_json"+ PS +"referral.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserAdminEntry()
   {
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   // TODO: move to the XML test suite
   /*
   void testXmlSerializerCompo()
   {
      Composition c = new Composition()
      
      c.name = new DvText(value: 'clinical document')
      c.archetype_node_id = 'openEHR-EHR-COMPOSITION.doc.v1'
      c.uid = new HierObjectId(value: UUID.randomUUID())
      
      c.language = new CodePhrase()
      c.language.code_string = '1234'
      c.language.terminology_id = new TerminologyId(value:'LOCAL')
      
      c.territory = new CodePhrase()
      c.territory.code_string = 'UY'
      c.territory.terminology_id = new TerminologyId(value:'LOCAL')
      
      c.category = new DvCodedText()
      c.category.value = 'event'
      c.category.defining_code = new CodePhrase()
      c.category.defining_code.code_string = '531'
      c.category.defining_code.terminology_id = new TerminologyId(value:'openehr')
           
      c.archetype_details = new Archetyped()
      c.archetype_details.archetype_id = new ArchetypeId(value: c.archetype_node_id)
      c.archetype_details.template_id = new TemplateId()
      c.archetype_details.template_id.value = 'doc.en.v1'
      
      c.context = new EventContext()
      c.context.start_time = new DvDateTime(value: '2021-01-14T10:10:00Z')
      c.context.location = 'location'
      
      c.context.setting = new DvCodedText()
      c.context.setting.value = 'setting'
      c.context.setting.defining_code = new CodePhrase()
      c.context.setting.defining_code.code_string = '12345'
      c.context.setting.defining_code.terminology_id = new TerminologyId(value:'LOCAL')
      
      AdminEntry a = new AdminEntry()
      
      a.language = new CodePhrase()
      a.language.code_string = 'ES'
      a.language.terminology_id = new TerminologyId(value:'LOCAL')
      
      a.encoding = new CodePhrase()
      a.encoding.code_string = 'UTF-8'
      a.encoding.terminology_id = new TerminologyId(value:'LOCAL')
      
      a.archetype_node_id = 'openEHR-EHR-ADMIN_ENTRY.test.v1'
      a.name = new DvText(value:'admin')
      a.data = new ItemTree(name: new DvText(value:'tree'))
      
      c.content.add(a)
      
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String out = serial.serialize(c)
      
      //println out
   }
   */
   
   
   void testJsonParserAdminEntryToXml()
   {
      // parse JSON
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      // serialize to XML
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String xml = serial.serialize(c)
      //println xml
      
      // validate xml
      def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
      def validator = new XmlInstanceValidation(inputStream)
      assert validateXMLInstance(validator, xml)
   }
   
   
   void testJsonParserInstructionToXml()
   {
      // parse JSON
      String path = PS +"canonical_json"+ PS +"lab_order.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      // serialize to XML
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String xml = serial.serialize(c)
      //println xml
      
      // validate xml
      def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
      def validator = new XmlInstanceValidation(inputStream)
      assert validateXMLInstance(validator, xml)
   }
   
   
   void testJsonParserObservationToXml()
   {
      // parse JSON
      String path = PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      // serialize to XML
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String xml = serial.serialize(c)
      //println xml
     
      // validate xml
      def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
      def validator = new XmlInstanceValidation(inputStream)
      assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserReferralWithParticipationsToXml()
   {
	  // parse JSON
	  String path = PS +"canonical_json"+ PS +"referral.json"
	  File file = new File(getClass().getResource(path).toURI())
	  String json = file.text
	  def parser = new OpenEhrJsonParser()
	  Composition c = (Composition)parser.parseJson(json)
	  
	  // serialize to XML
	  OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
	  String xml = serial.serialize(c)
	  //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserMinimalActionToXml()
   {
	  // parse JSON
	  String path = PS +"canonical_json"+ PS +"minimal_action.json"
	  File file = new File(getClass().getResource(path).toURI())
	  String json = file.text
	  def parser = new OpenEhrJsonParser()
	  Composition c = (Composition)parser.parseJson(json)
	  
	  // serialize to XML
	  OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
	  String xml = serial.serialize(c)
	  //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   
   void testJsonParserMinimalEvaluationToXml()
   {
	  // parse JSON
	  String path = PS +"canonical_json"+ PS +"minimal_evaluation.json"
	  File file = new File(getClass().getResource(path).toURI())
	  String json = file.text
	  def parser = new OpenEhrJsonParser()
	  Composition c = (Composition)parser.parseJson(json)
	  
	  // serialize to XML
	  OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
	  String xml = serial.serialize(c)
	  //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserNestedToXml()
   {
	  // parse JSON
	  String path = PS +"canonical_json"+ PS +"nested.json"
	  File file = new File(getClass().getResource(path).toURI())
	  String json = file.text
	  def parser = new OpenEhrJsonParser()
	  Composition c = (Composition)parser.parseJson(json)
	  
	  // serialize to XML
	  OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
	  String xml = serial.serialize(c)
	  //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserOximetriaToXml()
   {
	  // parse JSON
	  String path = PS +"canonical_json"+ PS +"oximetria_obs.json"
	  File file = new File(getClass().getResource(path).toURI())
	  String json = file.text
	  def parser = new OpenEhrJsonParser()
	  Composition c = (Composition)parser.parseJson(json)
	  
	  // serialize to XML
	  OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
	  String xml = serial.serialize(c)
	  //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserPhysicalActivityToXml()
   {
	  // parse JSON
	  String path = PS +"canonical_json"+ PS +"physical_activity.json"
	  File file = new File(getClass().getResource(path).toURI())
	  String json = file.text
	  def parser = new OpenEhrJsonParser()
	  Composition c = (Composition)parser.parseJson(json)
	  
	  // serialize to XML
	  OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
	  String xml = serial.serialize(c)
	  //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserProzedurToXml()
   {
	  // parse JSON
	  String path = PS +"canonical_json"+ PS +"prozedur.json"
	  File file = new File(getClass().getResource(path).toURI())
	  String json = file.text
	  def parser = new OpenEhrJsonParser()
	  Composition c = (Composition)parser.parseJson(json)
	  
	  // serialize to XML
	  OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
	  String xml = serial.serialize(c)
	  //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserAmdAssessmentToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"amd_assessment.en.v1.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParser()
     Composition c = (Composition)parser.parseJson(json)
     
     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserDiagnoseToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"diagnose.de.v1.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParser()
     Composition c = (Composition)parser.parseJson(json)
     
     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserExperimentalRespToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"experimental_respiratory_parameters_document.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParser()
     OriginalVersion v = (OriginalVersion)parser.parseVersionJson(json)
     
     def out = JsonOutput.toJson(v)
     out = JsonOutput.prettyPrint(out)
     //println out
     
     
     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(v)
     println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   void testJsonParserKorptempToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"intensivmedizinisches_monitoring_korpertemperatur.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParser()
     Composition c = (Composition)parser.parseJson(json)
     
     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml
     
     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlInstanceValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }
   
   static boolean validateXMLInstance(validator, xml)
   {
      if (!validator.validate(xml))
      {
         println 'NOT VALID'
         println '====================================='
         validator.errors.each {
            println it
         }
         println '====================================='
         
         return false
      }
      
      return true
   }
   
   void testJsonSchema()
   {
      //def uri = 'https://gist.githubusercontent.com/pieterbos/81651d2d7a5041a130ecb21b0a852e39/raw/2f31b9c7067bccf192256358da868ee8fbc7239a/OpenEHR%2520RM%2520json%2520schema,%2520with%2520default%2520instances%2520of%2520objects%2520addedcorrectly.json'
      //def jsonValidator = new JsonInstanceValidation(uri)
      def jsonValidator = new JsonInstanceValidation()

      ObjectMapper mapper = new ObjectMapper()

      // these files are loaded from the resources included from the JAR in the classpath
      def files = [
         'canonical_json/admin.json',
         'canonical_json/amd_assessment.en.v1.json',
         'canonical_json/diagnose.de.v1.json',
         'canonical_json/experimental_respiratory_parameters_document.json',
         'canonical_json/intensivmedizinisches_monitoring_korpertemperatur.json',
         'canonical_json/lab_order.json',
         'canonical_json/lab_results.json',
         'canonical_json/minimal_action_2.en.v1_instance_6866896.json',
         'canonical_json/minimal_action.json',
         'canonical_json/minimal_evaluation.json',
         'canonical_json/minimal_observation.en.v1_instance_7696347.json',
         'canonical_json/nested.json',
         'canonical_json/opt_tester.en.v1_instance_1482167.json',
         'canonical_json/oximetria_obs.json',
         'canonical_json/physical_activity.json',
         'canonical_json/prozedur.json',
         'canonical_json/referral.json',
         'canonical_json/test_all_datatypes_en.json',
         'canonical_json/vital_signs_pathfinder_demo.en.v1_instance_3602591.json'
      ]

      InputStream ins
      JsonNode json
      Set<ValidationMessage> errors

      // https://github.com/networknt/json-schema-validator/blob/master/src/test/java/com/networknt/schema/V7JsonSchemaTest.java
      files.each { testCaseFile ->

         //println testCaseFile

         //final URI testCaseFileUri = URI.create("classpath:" + testCaseFile)
         ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(testCaseFile)

         if (!ins) throw new Exception("Test file $testCaseFile not found")

         // https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html#readTree(java.io.InputStream)
         json = mapper.readTree(ins)
         errors = jsonValidator.validate(json)

         def out = JsonOutput.toJson(errors)
         out = JsonOutput.prettyPrint(out)
         println out
      }

   }

   void testCompositionJsonParseValidationSerializationValidation()
   {
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text

      // JSON VALIDATION
      def jsonValidator = new JsonInstanceValidation()
      //ObjectMapper mapper = new ObjectMapper()
      //InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)
      //JsonNode json = mapper.readTree(ins)
      Set<ValidationMessage> errors = jsonValidator.validate(json)

      assert errors.size() == 0

      // JSON PARSE
      def parser = new OpenEhrJsonParser()
      Composition compo = (Composition)parser.parseJson(json)

      // TODO: COMPOSITION VALIDATION AGAINST OPT

      // JSON SERIALIZATION
      def serializer = new OpenEhrJsonSerializer()
      String json2 = serializer.serialize(compo)
      errors = jsonValidator.validate(json2)

      // JSON VALIDATION
      def out = JsonOutput.toJson(errors)
      out = JsonOutput.prettyPrint(out)
      //println out

      println json2

      assert errors.size() == 0
   }
}
