/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.ektorp.CouchDbConnector;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.datastructs.Report;

/**
 *
 * @author Louis
 */
class CouchDBCallable implements Callable<Report> {

    private CouchDbConnector db;
    private Iterable<StorageInputStream> itrblStorageInputStream = null;
    private String fileName;

    public CouchDBCallable(Iterable<StorageInputStream> itrbl, CouchDbConnector pDb, String fileName) {
        super();
        this.itrblStorageInputStream = itrbl;
        this.db = pDb;
        this.fileName = fileName;
    }

    public Report call() throws Exception {
        if (itrblStorageInputStream == null) {
            return null;
        }
        for (StorageInputStream stream : itrblStorageInputStream) {
            BufferedWriter bufWriter;
            FileWriter fileWriter;
            String SOPInstanceUID;
            long start, end;
            start = System.currentTimeMillis();
            try {
                DicomInputStream dis = new DicomInputStream(stream.getInputStream());
                DicomObject dicomObj = dis.readDicomObject();
                dis.close();

                SOPInstanceUID = dicomObj.get(Tag.SOPInstanceUID).getValueAsString(dicomObj.getSpecificCharacterSet(), 0);
                if (!db.contains(SOPInstanceUID)) {
                    HashMap<String, Object> map = retrieveHeader(dicomObj);
                    map.put("_id", SOPInstanceUID);
                    db.create(map);
                }

                System.out.println("Indexed from " + stream.getURI());
                end = System.currentTimeMillis();
                fileWriter = new FileWriter(fileName, true);
                bufWriter = new BufferedWriter(fileWriter);
                bufWriter.newLine();
                bufWriter.write(String.format("%s %d %d", SOPInstanceUID, start, end));
                bufWriter.close();
                fileWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(CouchDBIndexer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new Report();
    }

    private HashMap<String, Object> retrieveHeader(DicomObject dicomObject) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Dictionary dicoInstance = Dictionary.getInstance();
        Iterator iter = dicomObject.datasetIterator();
        while (iter.hasNext()) {
            DicomElement element = (DicomElement) iter.next();
            int tag = element.tag();
            if (tag == Tag.PixelData) {
                continue;
            }
            try {
                String tagName = dicoInstance.tagName(tag);
                if (tagName == null) {
                    tagName = dicomObject.nameOf(tag);
                }
                if (dicomObject.vrOf(tag).toString().equals("SQ")) {
                    if (element.hasItems()) {
                        map.putAll(retrieveHeader(element.getDicomObject()));
                        continue;
                    }
                }
                DicomElement dicomElt = dicomObject.get(tag);
                String tagValue = dicomElt.getValueAsString(dicomObject.getSpecificCharacterSet(), 0);
                if (tagValue == null) {
                    continue;
                }
                Object obj;
                try {
                    obj = Double.parseDouble(tagValue);
                } catch (NumberFormatException e) {
                    obj = tagValue;
                }
                map.put(tagName, obj);
            } catch (Exception e) {
            }
        }
        return map;
    }
}
