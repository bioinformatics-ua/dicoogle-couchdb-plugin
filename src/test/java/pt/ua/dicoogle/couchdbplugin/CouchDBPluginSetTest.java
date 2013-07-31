/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.dcm4che2.io.DicomInputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.datastructs.Report;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.Task;

/**
 *
 * @author Louis
 */
public class CouchDBPluginSetTest {
    
    private static CouchDBPluginSet plugin;
    private static String dcmToTestPath = "D:\\DICOM_data\\datasetDCM\\Cardiac\\IM-0001-0031.dcm435509b7-1130-4f72-9c72-9ad60ae4c34e.dcm";
    private static String QUERY = "";
    //private static String SOPInstanceUID = "1.3.12.2.1107.5.6.1.123.6.0.59545429011895";
    private static String SOPInstanceUID = "1.3.12.2.1107.5.6.1.123.6.0.5";
    
    public CouchDBPluginSetTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        plugin = new CouchDBPluginSet();
        ConfigurationHolder settings;
        String pathConfigFile = ".\\settings\\couchdbplugin.xml";
        try {
            settings = new ConfigurationHolder(new File(pathConfigFile));
        } catch (ConfigurationException ex) {
            System.out.println("Error while loading configuration fil : " + pathConfigFile);
            return;
        }
        plugin.setSettings(settings);
    }
    
    /*
     * This test test all plugins of CouchDBPluginSet
     * It will do :
     *      - Store a dicom file from HDD to storage database
     *      - Index this dicom file from storage database to index database
     *      - Test a query
     *      - Unindex this dicom file from index database
     *      - Remove this dicom file from storage database
     */
    @Test
    public void testPlugin() throws IOException {
        System.out.println("Test all plugins");
        URI temp;
        List<StorageInterface> listStoragePlugins = (List<StorageInterface>) plugin.getStoragePlugins();
        if (!listStoragePlugins.isEmpty()) {
            File file = new File(dcmToTestPath);
            DicomInputStream inputStream = new DicomInputStream(file);
            // Store a dicom file from HDD to storage database
            temp = listStoragePlugins.get(0).store(inputStream);
            // Test indexing this dicom file
            if (!plugin.getIndexPlugins().isEmpty()) {
                // Index this dicom file from storage database to index database
                Task<Report> task = plugin.getIndexPlugins().get(0).index(listStoragePlugins.get(0).at(temp));
                task.run();
                // Test query
                if(!plugin.getQueryPlugins().isEmpty()){
                    //QUERY = "BitsStored:11";
                    QUERY = "SOPInstanceUID:"+SOPInstanceUID;
                    List<SearchResult> listResult = (List<SearchResult>) plugin.getQueryPlugins().get(0).query(QUERY);
                    System.out.println("Nb result : " + listResult.size());
                }
                // Unindex (remove) this dicom file from index database
                plugin.getIndexPlugins().get(0).unindex(temp);
            }
            // Remove this dicom file from storage database
            listStoragePlugins.get(0).remove(temp);
        }
        System.out.println("Test done");
    }
}