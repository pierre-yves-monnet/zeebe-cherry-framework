/* ******************************************************************** */
/*                                                                      */
/*  RunnerDecorationTemplate                                            */
/*                                                                      */
/*  Generate the template file used in Modeler from a runner            */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import com.google.gson.Gson;

import java.lang.reflect.Method;
import java.util.*;

public class RunnerDecorationTemplate {

    /*    We want to keep the Output parameter at the end.
    * when a field has a group in the list, it is placed in top.
    * When a field does not have a group, it is placed after in a group "custom properties"
    * so, we assign a Input group or an Output group by default to each field.
    * */

    public static final String GROUP_OUTPUT = "Output";
    public static final String GROUP_INPUT = "Input";

    private final AbstractRunner runner;


    public RunnerDecorationTemplate(AbstractRunner runner) {
        this.runner = runner;
    }

    public static String getJsonFromList(List<Map<String, Object>> listTemplates) {
        // transform the result in JSON
        Gson gson = new Gson();
        return gson.toJson(listTemplates);
    }

    /**
     * produce a JSON string containing the definition for the template
     * https://docs.camunda.io/docs/components/modeler/desktop-modeler/element-templates/defining-templates/
     *
     * @return the template
     */

    public Map<String, Object> getTemplate() {

        Map<String, Object> templateContent = new HashMap<>();
        if (! verifyRunner())
            return templateContent;

        templateContent.put("$schema", "https://unpkg.com/@camunda/zeebe-element-templates-json-schema/resources/schema.json");
        templateContent.put("name", runner.getDisplayLabel());
        templateContent.put("id", runner.getClass().getName());
        templateContent.put("description", runner.getDescription());
        templateContent.put("documentationRef", "https://docs.camunda.io/docs/components/modeler/web-modeler/connectors/available-connectors/template/");
        if (runner.getLogo() != null)
            templateContent.put("icon", Map.of("contents", runner.getLogo()));
        templateContent.put("category", Map.of("id", "connectors", "name", "connectors"));
        templateContent.put("appliesTo", List.of("bpmn:Task"));
        templateContent.put("elementType", Map.of("value", "bpmn:ServiceTask"));
        // no groups at this moment
        List<Map<String, Object>> listProperties = new ArrayList<>();
        templateContent.put("properties", listProperties);
        listProperties.add(
                Map.of("type", "Hidden",
                        "value", runner.getType(),
                        "binding", Map.of("type", "zeebe:taskDefinition:type")));
        boolean pleaseAddOutputGroup=false;
        if (runner instanceof AbstractConnector connector) {
            pleaseAddOutputGroup=true;

            // there is here two options:
            // connector return and object or a list of output
            if (runner.getListOutput().isEmpty()) {
                // connector returns an object
                listProperties.add(Map.of(
                        "label", "Result Variable",
                        "type", "String",
                        "value", "result",
                        "group", "output",
                        "binding", Map.of("type", "zeebe:taskHeader", "key", "resultVariable")));
            }
            else {
                listProperties.add(Map.of(
                        "type", "Hidden",
                        "value", "result",
                        "binding", Map.of("type", "zeebe:taskHeader", "key", "resultVariable")
                ));
            }
        }
        // Identify all groups
        List<RunnerParameter.Group> listGroups = new ArrayList<>();
        listGroups.addAll(runner.getListInput().stream()
                .filter(w -> w.group != null)
                .map(w -> w.group)
                .toList());

        // We group all result in a Group Input
        if (!runner.getListInput().isEmpty())
            listGroups.add(new RunnerParameter.Group(GROUP_INPUT, "Input"));

        // We group all result in a Group Output
        if (!runner.getListOutput().isEmpty() || pleaseAddOutputGroup)
            listGroups.add(new RunnerParameter.Group(GROUP_OUTPUT, "Output"));


        if (listGroups != null) {
            templateContent.put("groups",
                    listGroups.stream()
                            .distinct()
                            .map(w -> Map.of("id", w.id(), "label", w.label()))
                            .toList());
        }

        for (RunnerParameter runnerParameter : runner.getListInput()) {
            // do not generate a propertie for a accessAllVariables
            if (runnerParameter.isAccessAllVariables())
                continue;
            listProperties.addAll(getParameterProperties(runnerParameter, true,""));
        }
        for (RunnerParameter runnerParameter : runner.getListOutput()) {
            // do not generate a property for accessAllVariables
            if (runnerParameter.isAccessAllVariables())
                continue;
            listProperties.addAll(getParameterProperties(runnerParameter, false, runner instanceof AbstractConnector? "result.":""));
        }
        return templateContent;
    }

    /**
     * Get the template from a runnerParameter
     *
     * @param runnerParameter runner parameter to get the description
     * @param isInput         true if this is an input parameter
     * @param prefixName add a prefixName to the source in the binding (mandatory for connectors)
     * @return a template description
     */
    private List<Map<String, Object>> getParameterProperties(RunnerParameter runnerParameter, boolean isInput, String prefixName) {
        List<Map<String, Object>> listProperties = new ArrayList<>();

        // Calculate the condition
        HashMap<String, Object> condition = null;
        if (runnerParameter.conditionProperty != null) {
            condition = new HashMap<>();
            condition.put("property", runnerParameter.conditionProperty);
            condition.put("oneOf", runnerParameter.conditionOneOf);
        }
        /**
         * To have a checkbox, the parameter must be optionnal AND does not have already a condition
         */
        boolean addConditionCheckbox = (runnerParameter.conditionProperty == null) &&
                (RunnerParameter.Level.OPTIONAL.equals(runnerParameter.getLevel()));

        if (runnerParameter.visibleInTemplate)
            addConditionCheckbox = false;
        // Add the condition for all output
        if (!isInput)
            addConditionCheckbox = true;

        // is the parameter is optional? Add a checkbox first
        if (addConditionCheckbox) {
            Map<String, Object> propertyCheckbox = new HashMap<>();
            listProperties.add(propertyCheckbox);
            // the ID property is the value to link with the conditional
            propertyCheckbox.put("id", runnerParameter.name + "_optional");
            if (isInput)
                propertyCheckbox.put("label", "Provide " + runnerParameter.label + "?");
            else
                propertyCheckbox.put("label", "Saved " + runnerParameter.label + "?");
            // don't have the group at this moment
            propertyCheckbox.put("description", runnerParameter.explanation);
            propertyCheckbox.put("value", "false");
            propertyCheckbox.put("binding", Map.of("type", "zeebe:input",
                    "name", runnerParameter.name + "_optional"));

            propertyCheckbox.put("type", "Dropdown");
            List<Map<String, String>> listYesNoChoices = new ArrayList<>();
            listYesNoChoices.add(Map.of("name", "Yes", "value", "true"));
            listYesNoChoices.add(Map.of("name", "No", "value", "false"));
            propertyCheckbox.put("choices", listYesNoChoices);

            // if the parameters has a condition, add it here
            if (condition != null)
                propertyCheckbox.put("condition", condition);
            if (runnerParameter.group != null)
                propertyCheckbox.put("group", runnerParameter.group.id());
            propertyCheckbox.put("group", isInput? GROUP_INPUT:GROUP_OUTPUT);
        }

        Map<String, Object> propertyParameter = new HashMap<>();
        listProperties.add(propertyParameter);
        propertyParameter.put("id", runnerParameter.name);
        propertyParameter.put("label", runnerParameter.label);
        // don't have the group at this moment
        propertyParameter.put("description", runnerParameter.explanation);
        if (runnerParameter.defaultValue != null) {
            propertyParameter.put("value", runnerParameter.defaultValue);
        }
        String typeParameter = "String";
        // String, Text, Boolean, Dropdown or Hidden)
        if (Boolean.class.equals(runnerParameter.clazz)) {
            typeParameter = "Dropdown";
            List<Map<String, String>> listYesNoChoices = new ArrayList<>();
            listYesNoChoices.add(Map.of("name", "Yes", "value", "true"));
            listYesNoChoices.add(Map.of("name", "No", "value", "false"));
            propertyParameter.put("choices", listYesNoChoices);
        } else if (runnerParameter.hasChoice()) {
            typeParameter = "Dropdown";
            // add choices
            List<Map<String, String>> listChoices = new ArrayList<>();
            for (RunnerParameter.WorkerParameterChoice oneChoice : runnerParameter.workerParameterChoiceList) {
                listChoices.add(Map.of("name", oneChoice.displayName,
                        "value", oneChoice.code));
            }
            propertyParameter.put("choices", listChoices);
        }
        propertyParameter.put("type", typeParameter);
        if (isInput) {
            propertyParameter.put("binding",
                    Map.of("type", "zeebe:input",
                            "name", runnerParameter.name));
        } else {
            propertyParameter.put("binding",
                    Map.of("type", "zeebe:output",
                            "source", "= " + prefixName+ runnerParameter.name));

        }
        if (runnerParameter.group != null)
            propertyParameter.put("group", runnerParameter.group.id());
        else
            propertyParameter.put("group", isInput? GROUP_INPUT : GROUP_OUTPUT);

        Map<String, Object> constraints = new HashMap<>();
        // if the designer decide to show this property, then it is mandatory
        if (!isInput)
            constraints.put("notEmpty", Boolean.TRUE);

        if (RunnerParameter.Level.REQUIRED.equals(runnerParameter.level))
            constraints.put("notEmpty", Boolean.TRUE);

        if (!constraints.isEmpty())
            propertyParameter.put("constraints", constraints);

        // if this is a OPTIONAL, then the display depends on the check box.
        // if there is a condition on the OPTIONAL, then the condition is part of the checkbox, else will be on the parameters
        if (addConditionCheckbox) {
            propertyParameter.put("condition", Map.of("property", runnerParameter.name + "_optional",
                    "equals", "true"));

        } else {
            if (condition != null)
                propertyParameter.put("condition", condition);

        }


        return listProperties;
    }

    private boolean verifyRunner() {
        StringBuilder errors= new StringBuilder();
        if (runner instanceof AbstractConnector runnerConnector) {
            AbstractConnectorOutput abstractConnectorOutput = runnerConnector.getAbstractConnectorOutput();
            // ATTENTION, the output must start with a lower case, and
            for (RunnerParameter runnerParameter : runner.getListOutput()) {
                // do not generate a property for accessAllVariables
                if (runnerParameter.isAccessAllVariables())
                    continue;
                if (runnerParameter.getName().isEmpty()) {
                    errors.append("One parameters does not have a name");
                    continue;
                }
                String firstLetter = runnerParameter.getName().substring(0,1);

                if (! firstLetter.toLowerCase().equals(firstLetter)) {
                    errors.append("The first letter must be in Lower case");
                    continue;
                }
                // check if a method get<runnerParameter.getName()> exist
                Method m = null;
                try {
                    m = abstractConnectorOutput.getClass().getMethod("get"+runnerParameter.getName(), null);
                } catch (NoSuchMethodException e) {
                    errors.append("A method [get"+runnerParameter.getName()+"()] must exist");
                    continue;
                }


            }
        }
        // where to log the information??

        return errors.isEmpty();
    }
}
