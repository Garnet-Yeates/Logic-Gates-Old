package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;

import javax.swing.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        if (gui != null)
            gui.updateSaveMenuBars();
    }

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public void writeToFile(File f) {
        XMLStreamWriter writer = null;
        try {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            FileWriter fw = new FileWriter(f);
            writer = factory.createXMLStreamWriter(fw);
            writer.writeStartDocument();
            writeLnTab(writer, 0);
            writer.writeStartElement("project");
            writer.writeAttribute("name", projectName);
            writeLnTab(writer, 1);
            for (Circuit c : circuits) {
                writer.writeStartElement("circuit");
                writer.writeAttribute("name", c.getCircuitName());
                for (Entity e : c.getAllEntities()) {
                    writeLnTab(writer, 2);
                    writer.writeStartElement("entity");
                    writer.writeCharacters(e.toParsableString());
                    writer.writeEndElement();
                }
                writeLnTab(writer, 1);
                writer.writeEndElement();
            }
            writeLnTab(writer, 0);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            fw.close();
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            if (writer != null) {
                try {
                    writer.close();
                } catch (XMLStreamException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void writeLnTab(XMLStreamWriter writer, int tabs) throws XMLStreamException {
        StringBuilder tabString = new StringBuilder();
        tabString.append("    ".repeat(Math.max(0, tabs)));
        writer.writeCharacters("\n" + tabString + "");
    }

    public static Project fromFile(File file) throws LoadFailedException {
        Project p = new Project(null);
        p.setPath(file.getPath());
        XMLEventReader eventReader = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            eventReader = factory.createXMLEventReader(new FileReader(file));

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
                                        String local = at.getName().getLocalPart();
                                        if (local.equalsIgnoreCase("name")) {
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
                                    currCircuit.disableUpdate();
                                    currCircuit.enablePowerUpdateBuffer();
                                    break;
                            }
                            break;
                        case XMLStreamConstants.CHARACTERS:
                            Characters characters = event.asCharacters();
                            System.out.println(characters.getData());
                            if (project && circuit && parseEntity) {
                                System.out.println("  parse " + characters.getData());
                                    currCircuit.addEntity(Entity.parseEntity(currCircuit, true, characters.getData()));

                            }
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
                                    if (currCircuit == null)
                                        throw new RuntimeException();
                                    currCircuit.enableUpdate();
                                    currCircuit.getAllEntities().forEach(Entity::update);
                                    currCircuit.disableAndPollPowerUpdateBuffer();
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
            if (eventReader != null) {
                try {
                    eventReader.close();
                } catch (XMLStreamException ignored) { }
            }
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
        currCircuit.stateController().updateMenuBars();
        currCircuit.selectionTableUpdate();
    }

    public Circuit getCurrentCircuit() {
        return currCircuit;
    }
}
