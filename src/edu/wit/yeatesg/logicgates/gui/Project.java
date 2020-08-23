package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;

import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Project {

    private ArrayList<Circuit> circuits = new ArrayList<>();
    private MainGUI gui;
    private String path; // if !endsWith(name + ".lgp" throw)
    private Circuit currCircuit;
    private String projectName;

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public void toFile(File f) {
        try {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(new FileWriter(f));
            writer.writeStartDocument();
            writer.writeStartElement("project");
            writer.writeAttribute("name", projectName);
            for (Circuit c : circuits) {
                writer.writeStartElement("circuit");
                writer.writeAttribute("name", c.getCircuitName());
                for (Entity e : c.getAllEntities()) {
                    writer.writeStartElement("entity");
                    writer.writeCharacters(e.toParsableString());
                    writer.writeEndElement();;
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();

        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
        }
    }

    public static Project fromFile(File file) throws LoadFailedException {
        Project p = new Project(null);
        p.path = file.getPath();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader =
                    factory.createXMLEventReader(new FileReader(file));

            boolean project = false;
            boolean parseEntity = false;
            boolean circuit = false;

            Circuit currCircuit = null;

            while (eventReader.hasNext()) {
                try {
                    XMLEvent event = eventReader.nextEvent();
                    switch (event.getEventType()) {
                        case XMLStreamConstants.START_ELEMENT:
                            StartElement startElement = event.asStartElement();
                            String qName = startElement.getName().getLocalPart();
                            switch (qName) {
                                case "project":
                                    project = true;
                                    startElement.getAttributes().forEachRemaining((at) -> {
                                        if (at.getName().getLocalPart().equals("name")) {
                                            p.projectName = at.getValue();
                                        }
                                    });
                                    break;
                                case "entity":
                                    parseEntity = true;
                                    break;
                                case "circuit":
                                    circuit = true;
                                    ArrayList<Attribute> attributes = new ArrayList<>();
                                    String circuitName = null;
                                    startElement.getAttributes().forEachRemaining(attributes::add);
                                    for (Attribute at : attributes) {
                                        if (at.getName().getLocalPart().equalsIgnoreCase("name"))
                                            circuitName = at.getValue();
                                    }
                                    currCircuit = new Circuit(p, circuitName);
                                    break;
                            }
                            break;
                        case XMLStreamConstants.CHARACTERS:
                            Characters characters = event.asCharacters();
                            if (project && circuit && parseEntity)
                                currCircuit.addEntity(Entity.parseEntity(currCircuit, true, characters.getData()));
                            break;
                        case XMLStreamConstants.END_ELEMENT:
                            EndElement endElement = event.asEndElement();
                            String endElementName = endElement.getName().getLocalPart();
                            switch (endElementName) {
                                case "project":
                                    project = false;
                                    break;
                                case "entity":
                                    parseEntity = false;
                                    break;
                                case "circuit":
                                    circuit = false;
                                    currCircuit = null;
                                    break;
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new LoadFailedException("Corrupt Circuit (.cxml) File");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new LoadFailedException("Invalid File");
        }
        return p;
    }

    public static class LoadFailedException extends Exception {
        private String message;
        public LoadFailedException(String message) { this.message = message; }
        public String getMessage() { return message; }
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
        if (c == null)
            throw new RuntimeException();
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
