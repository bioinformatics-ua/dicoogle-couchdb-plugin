/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.ektorp.AttachmentInputStream;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 *
 * @author Louis
 */
public class CouchDBStorage implements StorageInterface {

    private boolean isEnable = false;
    private ConfigurationHolder settings = null;
    private URI location;
    private CouchDbConnector db;
    private String host;
    private String dbName;
    private int port;
    private static String hostKey = "DefaultServerHost";
    private static String portKey = "DefaultServerPort";
    private static String dbNameKey = "StorageDataBase";

    public CouchDBStorage() {
        System.out.println("Init -> CouchDB Storage");
    }

    @Override
    public String getScheme() {
        if (this.settings == null) {
            return null;
        }
        return "http://" + host + ":" + port + "/" + dbName + "/";
    }

    @Override
    public boolean handles(URI uri) {
        CouchDBURI temp = new CouchDBURI(uri);
        return temp.verify();
    }

    @Override
    public Iterable<StorageInputStream> at(URI pUri) {
        CouchDBURI uri = new CouchDBURI(pUri);
        if (!isEnable || !uri.verify() || CouchDBPluginSet.server == null) {
            return null;
        }
        ArrayList<StorageInputStream> list = new ArrayList<StorageInputStream>();
        CouchDBStorageInputStream couchDBStorageIn = new CouchDBStorageInputStream(pUri);
        list.add(couchDBStorageIn);
        return list;
    }

    @Override
    public URI store(DicomObject dicomObject) {
        if (!isEnable || CouchDBPluginSet.server == null || dicomObject == null) {
            return null;
        }
        String fileName = dicomObject.get(Tag.SOPInstanceUID).getValueAsString(dicomObject.getSpecificCharacterSet(), 0);
        URI uri;
        try {
            uri = new URI(this.location + fileName);
        } catch (URISyntaxException e) {
            System.out.println("Error : URISyntaxException");
            return null;
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DicomOutputStream dos = new DicomOutputStream(os);
            dos.writeDicomFile(dicomObject);

            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            StorageDocument doc;
            if (db.contains(fileName)) {
                doc = db.get(StorageDocument.class, fileName);
            } else {
                doc = new StorageDocument();
                doc.setId(fileName);
            }
            AttachmentInputStream attIn = new AttachmentInputStream(doc.getId() + ".dcm", is, "binary/dicom");
            db.createAttachment(doc.getId(), doc.getRevision(), attIn);
            attIn.close();
        } catch (IOException ex) {
            Logger.getLogger(CouchDBStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Stored at " + uri);
        return uri;
    }

    @Override
    public URI store(DicomInputStream stream) throws IOException {
        if (stream == null) {
            return null;
        }
        return store(stream.readDicomObject());
    }

    @Override
    public void remove(URI pUri) {
        CouchDBURI uri = new CouchDBURI(pUri);
        if (!isEnable || !uri.verify() || CouchDBPluginSet.server == null) {
            return;
        }
        try {
            StorageDocument doc = db.get(StorageDocument.class, uri.getFileName());
            db.delete(doc);
        } catch (DocumentNotFoundException e) {
            System.out.println(pUri + "not found");
        }
    }

    @Override
    public String getName() {
        return "couchdbplugin";
    }

    @Override
    public boolean enable() {
        if (CouchDBPluginSet.server == null || this.settings == null) {
            return false;
        }
        try {
            location = new URI("http://" + host + ":" + port + "/" + dbName + "/");
        } catch (URISyntaxException e) {
            return false;
        }
        isEnable = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.isEnable = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnable;
    }

    @Override
    public void setSettings(ConfigurationHolder ch) {
        this.settings = ch;
        host = settings.getConfiguration().getString(hostKey);
        port = settings.getConfiguration().getInt(portKey);
        dbName = this.settings.getConfiguration().getString(dbNameKey);
        this.db = CouchDBPluginSet.server.createConnector(dbName, true);
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }
}
