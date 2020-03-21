package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.entity.Entity;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.*;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Project {

    private ArrayList<Circuit> circuits = new ArrayList<>();
    private MainGUI gui;
    private String name;
    private String path; // if !endsWith(name + ".lgp" throw)
    private Circuit currCircuit;
    private String projectName;

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public static Project fromFile(File file) throws LoadFailedException {
        Project project = new Project(null);
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader =
                    factory.createXMLEventReader(new FileReader(file));

            boolean addingToProject = false;

            ArrayList<Entity> entityDefaults = new ArrayList<>();

            boolean addingDefaults = false;
            boolean addingEntitiesToCircuit = false;
            boolean addingEntitiesToDefaults = false;
            boolean readingEntityParse = false;

            boolean addingCircuits = false;
            Circuit circuitBeingAdded = null;

            while (eventReader.hasNext()) {
                try {
                    XMLEvent event = eventReader.nextEvent();

                    switch (event.getEventType()) {
                        case XMLStreamConstants.START_ELEMENT:
                            StartElement startElement = event.asStartElement();
                            String qName = startElement.getName().getLocalPart();
                            if (qName.equalsIgnoreCase("project")) {
                                addingToProject = true;
                                startElement.getAttributes().forEachRemaining((at) -> {
                                    if (at.getName().getLocalPart().equals("projectName"))
                                        project.name = at.getValue();
                                });
                            }
                            else if (addingToProject) {
                                if (qName.equalsIgnoreCase("defaults"))
                                    addingDefaults = true;
                                else if (qName.equalsIgnoreCase("entities") && addingDefaults)
                                    addingEntitiesToDefaults = true;
                                else if (qName.equalsIgnoreCase("entities") && circuitBeingAdded != null)
                                    addingEntitiesToCircuit = true;
                                else if (qName.equalsIgnoreCase("circuits"))
                                    addingCircuits = true;
                                else if (qName.equalsIgnoreCase("entity"))
                                    readingEntityParse = true;
                                else if (qName.equalsIgnoreCase("circuit") && addingCircuits) {
                                    StringBuilder circuitName = new StringBuilder();
                                    startElement.getAttributes().forEachRemaining((at) -> {
                                        if (at.getName().getLocalPart().equals("circuitName"))
                                            circuitName.append(at.getValue());
                                    });
                                    circuitBeingAdded = new Circuit(project, circuitName.toString());
                                }
                            }
                            break;

                        case XMLStreamConstants.CHARACTERS:
                            if (readingEntityParse /* || other cases where we need chars */) {
                                Characters characters = event.asCharacters();
                                if (addingEntitiesToDefaults)
                                    ; // TODO implement later  entityDefaults.add(Entity.parseEntity(circuitBeingAdded, false, characters.getData()));
                                else if (addingEntitiesToCircuit && circuitBeingAdded != null)
                                    circuitBeingAdded.addEntity(Entity.parseEntity(circuitBeingAdded, true, characters.getData()));
                            }
                            break;

                        case XMLStreamConstants.END_ELEMENT:
                            EndElement endElement = event.asEndElement();
                            String endElementName = endElement.getName().getLocalPart();
                            if (endElementName.equalsIgnoreCase("project")) {
                                addingToProject = false;
                                // We are done
                            } else if (endElementName.equalsIgnoreCase("defaults")) {
                                project.setEntityDefaults(entityDefaults);
                                addingDefaults = false;
                            }
                            else if (endElementName.equalsIgnoreCase("entities") && addingDefaults)
                                addingEntitiesToDefaults = false;
                            else if (endElementName.equalsIgnoreCase("entities") && circuitBeingAdded != null)
                                addingEntitiesToCircuit = false;
                            else if (endElementName.equalsIgnoreCase("entity"))
                                readingEntityParse = false;
                            else if (endElementName.equalsIgnoreCase("circuits")) {
                                addingCircuits = false;
                            } else if (endElementName.equalsIgnoreCase("circuit")) {
                                project.addCircuit(circuitBeingAdded);
                                circuitBeingAdded = null;
                            }
                            break;
                    }
                } catch (Exception e) {
                    throw new LoadFailedException("Corrupt Circuit (.cxml) File");
                }
            }
        } catch (Exception e) {
            throw new LoadFailedException("Invalid File");
        }
        return project;
    }


    public static class LoadFailedException extends Exception {
        private String message;
        public LoadFailedException(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    private void setEntityDefaults(ArrayList<Entity> entityDefaults) {
    }

    public void setGUI(MainGUI gui) {
        this.gui = gui;
    }

    public MainGUI getGUI() {
        return gui;
    }

    public String getProjectName() {
        return projectName;
    }

    public ArrayList<Circuit> getCircuits() {
        return circuits;
    }

    public boolean hasCircuits() {
        return circuits.size() > 0;
    }

    public void addCircuit(Circuit c) {
        if (!c.getCircuitName().contains("theoretical")) {
            for (Circuit other : getCircuits())
                if (other.getCircuitName().equalsIgnoreCase(c.getCircuitName()))
                    throw new RuntimeException("Duplicate Circuit \"" + c.getCircuitName()
                            + "\" On Project \"" + getProjectName() + "\"");
            circuits.add(c);
            if (circuits.size() == 1)
                setCurrentCircuit(c); // If this is the only circuit, obviously we want it to be the current one
        }
    }

    public EditorPanel getEditorPanel() {
        return gui.getEditorPanel();
    }

    public void setCurrentCircuit(Circuit c) {
        boolean foundMatch = false;
        for (Circuit circuit : circuits) {
            if (circuit == c) {
                foundMatch = true;
                break;
            }
        }
        if (!foundMatch)
            throw new RuntimeException("Can't set current circuit to this circuit; doesn't exist in the Project instance");
        currCircuit = c;
    }

    public Circuit getCurrentCircuit() {
        return currCircuit;
    }
}
