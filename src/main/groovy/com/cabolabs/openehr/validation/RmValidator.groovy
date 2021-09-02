package com.cabolabs.openehr.validation

import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.rm_1_0_2.composition.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.*
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.*
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.validation.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

class RmValidator {

   OptManager opt_manager

   RmValidator(OptManager opt_manager)
   {
      this.opt_manager = opt_manager
   }

   // the namespace is where the OPT is stored/cached, allows to implement multi-tenancy
   RmValidationReport dovalidate(Composition c, String namespace)
   {
      String template_id = c.archetype_details.template_id.value

      def opt = this.opt_manager.getOpt(template_id, namespace)

      if (!opt) return opt_not_found(template_id)

      return validate(c, opt.definition)
   }

   private RmValidationReport validate(Composition c, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate(c, c.category, o.getAttr('category'), "/category"))

      // the attributes that are optional in the opt should be checked by the parent to avoid calling
      // with null because polymorphism can't find the right method. Also if the constraint is null,
      // anything matches.
      def a_context = o.getAttr('context')
      if (a_context)
      {
         if (c.context)
         {
            report.append(validate(c.context, a_context))
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_context.existence.has(0))
            {
               report.addError("/context", "attribute is not present but is required")
            }
         }
      }

      def a_content = o.getAttr('content')
      if (a_content)
      {
         if (c.content != null)
         {
            report.append(validate(c.content, a_content)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_content.existence.has(0))
            {
               report.addError("/content", "attribute is not present but is required")
            }
         }
      }

      return report
   }

   // all container attributes will get here
   private RmValidationReport validate(List container, AttributeNode c_multiple_attribute)
   {
      /*
      println "validate container attribute "
      println c_multiple_attribute.templatePath
      println c_multiple_attribute.templateDataPath
      println c_multiple_attribute.dataPath
      */
      RmValidationReport report = new RmValidationReport()

      if (!c_multiple_attribute.cardinality.interval.has(container.size()))
      {
         // cardinality error
         // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
         report.addError(c_multiple_attribute.templateDataPath, "Number of objects in container ${container.size()} doesn't match cardinality constraint "+ c_multiple_attribute.cardinality.interval.toString())
      }

      // existence
      if (c_multiple_attribute.existence)
      {
         // FIXME: all attribute existence should be validated in the parent object, since if it's null, it can't call the method
         def existence = (container ? 1 : 0)
         if (!c_multiple_attribute.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(c_multiple_attribute.templateDataPath, "Node doesn't match existence")
         }
      }

      // validate each item in the container if any
      // and if c_multiple_attribute has children (if not, any object is valid)
      if (c_multiple_attribute.children)
      {
         //println container*.value
         //println container*.archetype_node_id
         container.each { item ->

            //println "item "+ item.archetype_node_id
            //println c_multiple_attribute.children*.nodeId

            // each item in the collection should validate against the child object with the same node_id
            def obj = c_multiple_attribute.children.find{ 
               if (it.type == 'C_ARCHETYPE_ROOT')
                  it.archetypeId == item.archetype_node_id
               else
                  it.nodeId == item.archetype_node_id   
            }

            //println obj.type
            
            if (obj)
            {
               report.append(validate(item, obj))
            }
            else
            {
              println "No object node with node_id ${item.archetype_node_id}"
            }
         }
      }

      return report
   }

   private RmValidationReport validate_alternatives(Observation ob, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ob, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Observation ob, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ob ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // history

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ob.data) // this is mandatory by the RM
         {
            report.append(validate(ob.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      def a_state = o.getAttr('state') // history

      if (a_state) // if the attribute node is null, all objects validate
      {
         if (ob.state) // this is mandatory by the RM
         {
            report.append(validate(ob.state, a_state))
         }
         else
         {
            if (!a_state.existence.has(0))
            {
               report.addError(o.templateDataPath, "/state is not present but is required")
            }
         }
      }


      // TODO: protocol

      return report
   }

   private RmValidationReport validate(History h, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (h ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(h, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(History h, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(h, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(History h, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // TODO: not validating occurrences?

      def a_events = o.getAttr('events')
      if (a_events)
      {
         if (h.events != null)
         {
            report.append(validate(h.events, a_events)) // validate container
         }
         else
         {
            // if the container attribute is null the existence is validated in the parent object
            if (!a_events.existence.has(0))
            {
               report.addError(h.dataPath + "/events", "is not present but is required")
            }
         }
      }

      // TODO: summary

      return report
   }

   private RmValidationReport validate_alternatives(PointEvent e, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(e, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(PointEvent e, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item_structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (e.data) // this is mandatory by the RM
         {
            report.append(validate(e.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate_alternatives(IntervalEvent e, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(e, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(IntervalEvent e, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item_structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (e.data) // this is mandatory by the RM
         {
            report.append(validate(e.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate_alternatives(Evaluation ev, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ev, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Evaluation ev, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ev ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ev.data)
         {
            report.append(validate(ev.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }


      // TODO: protocol

      return report
   }

   private RmValidationReport validate_alternatives(Instruction ins, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ins, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Instruction ins, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ins ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }
      
      def a_activities = o.getAttr('activities') // List<Activity>
      if (a_activities)
      {
         if (ins.activities != null)
         {
            report.append(validate(ins.activities, a_activities)) // validate container
         }
         else
         {
            // if the container attribute is null the existence is validated in the parent object
            if (!a_activities.existence.has(0))
            {
               report.addError(ins.dataPath + "/activities", "is not present but is required")
            }
         }
      }

      // TODO: protocol

      return report
   }

   private RmValidationReport validate_alternatives(Action ac, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ac, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Action ac, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ac ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_description = o.getAttr('description') // item structure

      if (a_description) // if the attribute node is null, all objects validate
      {
         if (ac.description)
         {
            report.append(validate(ac.description, a_description))
         }
         else
         {
            // TODO: should validate existence if the attribute node is not null
            if (!a_description.existence.has(0))
            {
               report.addError(o.templateDataPath, "/description is not present but is required")
            }
         }
      }

      // TODO: protocol

      return report
   }

   private RmValidationReport validate_alternatives(AdminEntry ae, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ae, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(AdminEntry ae, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ae ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ae.data)
         {
            report.append(validate(ae.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(EventContext context, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (context ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(context, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(EventContext context, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(context, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(EventContext context, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      if (context.other_context)
      {
         report.append(context.other_context, o.getAttr('other_context'))
      }
      else
      {
         // TODO: the existence of the attribute when it's null should be checked by the parent
         // because polymorphism can't choose the right method to call with a null value
      }

      // TODO: is 'participations' archetypable?

      return report
   }
 
   private RmValidationReport validate(ItemTree is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemTree is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemTree is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_items = o.getAttr('items')
      if (a_items)
      {
         if (is.items != null)
         {
            report.append(validate(is.items, a_items)) // validate container
         }
         else
         {
            if (!a_items.existence.has(0))
            {
               report.addError(is.dataPath + "/items", "is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(ItemList is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemList is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemList is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_items = o.getAttr('items')
      if (a_items)
      {
         if (is.items != null)
         {
            report.append(validate(is.items, a_items)) // validate container
         }
         else
         {
            if (!a_items.existence.has(0))
            {
               report.addError(is.dataPath + "/items", "is not present but is required")
            }
         }
      }

      return report
   }



   private RmValidationReport validate(ItemTable is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemTable is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemTable is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_rows = o.getAttr('rows')
      if (a_rows)
      {
         if (is.rows != null)
         {
            report.append(validate(is.rows, a_rows)) // validate container
         }
         else
         {
            if (!a_rows.existence.has(0))
            {
               report.addError(is.dataPath + "/rows", "is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(ItemSingle is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemSingle is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemSingle is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      report.append(validate(is.item, o.getAttr('item')))

      return report
   }


   private RmValidationReport validate_alternatives(Cluster cl, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(cl, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Cluster cl, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (cl ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_items = o.getAttr('items')
      if (a_items)
      {
         if (cl.items != null)
         {
            report.append(validate(cl.items, a_items)) // validate container
         }
         else
         {
            if (!a_items.existence.has(0))
            {
               report.addError(cl.dataPath + "/items", "is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Element e, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (e ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(e, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(Element e, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(e, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Element e, ObjectNode o)
   {
      /*
      println 'element path '+ e.path
      println 'element data path '+ e.dataPath
      println 'object path '+ o.path
      println 'object template path '+ o.templatePath
      */

      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: need to pass the path to the DV* validators so the error reporting can
      // include the path, but should be the data path!

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (e.value)
         {
            report.append(validate(e, e.value, a_value, "/value"))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError(e.dataPath + "/value", "is not present but is required")
            }
         }
      }

      def a_null_flavour = o.getAttr('null_flavour')
      if (a_null_flavour)
      {
         if (e.null_flavour)
         {
            report.append(validate(e, e.null_flavour, a_null_flavour, "/null_flavour"))
         }
         else
         {
            // FIXME: existence is null for this attribute, check the OPTs and the parser,
            // since existence should be mandatory or have a default value from the RM
            // added the check for existence
            if (a_null_flavour.existence && !a_null_flavour.existence.has(0))
            {
               report.addError(e.dataPath + "/null_flavour", "is not present but is required")
            }
         }
      }


      return report
   }

   private RmValidationReport validate(Pathable parent, DvCodedText ct, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (ct ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, ct, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvCodedText ct, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, ct, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvCodedText ct, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ct ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_defining_code = o.getAttr('defining_code')
      if (a_defining_code)
      {
         if (ct.defining_code)
         {
            report.append(validate(parent, ct.defining_code, a_defining_code, dv_path + "/defining_code"))
         }
         else
         {
            if (!a_defining_code.existence.has(0))
            {
               report.addError("/defining_code", "is not present but is required")
            }
         }
      }

      // TODO: mappings

      return report
   }

   private RmValidationReport validate(Pathable parent, CodePhrase cp, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (cp ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, cp, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, CodePhrase cp, List<CCodePhrase> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, cp, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, CodePhrase cp, CCodePhrase o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (cp ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // specific type constraint validation
      ValidationResult valid = o.isValid(parent, cp.code_string, cp.terminology_id.value)

      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }

   private RmValidationReport validate(Pathable parent, DvText te, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (te ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this dv_path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, te, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvText te, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, te, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvText te, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (te ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: validate value string if there is any constrain

      return report
   }

   private RmValidationReport validate(Pathable parent, DvProportion d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvProportion d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvProportion d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: validate numerator and denominator if there is any constrain, and against the type

      return report
   }


   private RmValidationReport validate(Pathable parent, DvQuantity d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvQuantity d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         //println o.type +" "+ o.rmTypeName
         
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return new RmValidationReport() //report
   }

   private RmValidationReport validate(Pathable parent, DvQuantity d, CDvQuantity o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: validate magnitude, precision and units
      ValidationResult valid = o.isValid(parent, d.units, d.magnitude)
      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvCount d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvCount d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvCount d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: validate magnitude
      def a_magnitude = o.getAttr('magnitude')
      if (a_magnitude)
      {
         if (d.magnitude != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.magnitude, a_magnitude, dv_path +'/magnitude'))
         }
         else
         {
            if (!a_magnitude.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /magnitude is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Pathable parent, Integer d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, Integer d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, Integer d, PrimitiveObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      ValidationResult valid = o.item.isValid(parent, d) // item is CInteger

      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }



   private RmValidationReport validate(Pathable parent, DvDateTime d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvDateTime d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvDateTime d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvDate d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvDate d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvDate d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvTime d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvTime d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvTime d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, String d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, String d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, String d, PrimitiveObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      ValidationResult valid = o.item.isValid(parent, d) // item is could be CDateTime, CString, CDate, CDuration, etc.

      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvOrdinal d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvOrdinal d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvOrdinal d, CDvOrdinal o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvBoolean d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvBoolean d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvBoolean d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, Boolean d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, Boolean d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, Boolean d, PrimitiveObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      ValidationResult valid = o.item.isValid(parent, d) // item is could be CBoolean

      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvDuration d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvDuration d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvDuration d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvInterval d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvInterval d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvInterval d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // in the OPT the interval definition is a C_COMPLEX_OBJECT
      // it contains C_COMPLEX_OBJECT for the upper and lower constraints
      // each constraint could be a range for the magnitude, so we could have:
      // lower(a..b) .. upper(c..d) in the OPT
      // then in the data we have an interval (a1..b1) that is lower and higher
      // so we know a1 should be in a..b
      // and b2 should be in c..d

      def a_lower = o.getAttr('lower')
      if (a_lower)
      {
         if (d.lower != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.lower, a_lower, dv_path +'/lower'))
         }
         else
         {
            if (!a_lower.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /lower is not present but is required")
            }
         }
      }

      def a_upper = o.getAttr('upper')
      if (a_upper)
      {
         if (d.upper != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.upper, a_upper, dv_path +'/upper'))
         }
         else
         {
            if (!a_upper.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /upper is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvMultimedia d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvMultimedia d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvMultimedia d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      /* TODO: validate multimedia

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }
      */

      return report
   }


   private RmValidationReport validate(Pathable parent, DvParsable d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvParsable d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvParsable d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      /* TODO: validate DvParsable

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }
      */

      return report
   }


   // Same validators are used for DvEhrUri
   private RmValidationReport validate(Pathable parent, DvUri d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvUri d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvUri d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }
      
      return report
   }


   private RmValidationReport validate(Pathable parent, DvIdentifier d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvIdentifier d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvIdentifier d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_issuer = o.getAttr('issuer')
      if (a_issuer)
      {
         if (d.issuer != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.issuer, a_issuer, dv_path + '/issuer'))
         }
         else
         {
            if (!a_issuer.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /issuer is not present but is required")
            }
         }
      }

      def a_type = o.getAttr('type')
      if (a_type)
      {
         if (d.type != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.type, a_type, dv_path +'/type'))
         }
         else
         {
            if (!a_type.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /type is not present but is required")
            }
         }
      }

      def a_assigned = o.getAttr('assigner')
      if (a_assigned)
      {
         if (d.assigner != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.assigner, a_assigned, dv_path +'/assigned'))
         }
         else
         {
            if (!a_assigned.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /assigner is not present but is required")
            }
         }
      }
      
      return report
   }


   RmValidationReport opt_not_found(String template_id)
   {
      def report = new RmValidationReport()
      report.addError("Template '${template_id}' was not found")
      return report
   }
}