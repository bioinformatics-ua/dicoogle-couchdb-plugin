/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.datastructs.Report;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.Task;

/**
 *
 * @author Louis
 */
public class CouchDBIndexer implements IndexerInterface {

    private boolean isEnable;
    private ConfigurationHolder settings = null;
    private CouchDbConnector db;
    private static String fileName = "log.txt";
    private static String dbNameKey = "IndexDataBase";

    public CouchDBIndexer() {
        System.out.println("Init -> CouchDB Indexer");
    }

    @Override
    public Task<Report> index(Iterable<StorageInputStream> itrbl) {
        CouchDBCallable c = new CouchDBCallable(itrbl, db, fileName);
        Task<Report> task = new Task(c);
        return task;
    }

    @Override
    public Task<Report> index(StorageInputStream stream) {
        ArrayList<StorageInputStream> itrbl = new ArrayList<StorageInputStream>();
        itrbl.add(stream);
        CouchDBCallable c = new CouchDBCallable(itrbl, db, fileName);
        Task<Report> task = new Task(c);
        return task;
    }

    @Override
    public boolean unindex(URI uri) {
        if (this.settings == null || CouchDBPluginSet.server == null) {
            return false;
        }
        CouchDBURI uriTemp = new CouchDBURI(uri);
        try {
            HashMap<String, Object> map = db.get(HashMap.class, uriTemp.getFileName());
            db.delete(map);
        } catch (DocumentNotFoundException e) {
            System.out.println(uri + " not found");
        }
        return true;
    }

    @Override
    public String getName() {
        return "couchdbplugin";
    }

    @Override
    public boolean enable() {
        if (settings == null || CouchDBPluginSet.server == null) {
            return false;
        }
        this.isEnable = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.isEnable = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnable;
    }

    @Override
    public void setSettings(ConfigurationHolder ch) {
        this.settings = ch;
        String dbName = this.settings.getConfiguration().getString(dbNameKey);
        this.db = CouchDBPluginSet.server.createConnector(dbName, true);
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }
}
