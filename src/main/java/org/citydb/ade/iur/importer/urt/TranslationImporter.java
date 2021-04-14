package org.citydb.ade.iur.importer.urt;

import org.citydb.ade.importer.CityGMLImportHelper;
import org.citydb.ade.iur.importer.ImportManager;
import org.citydb.ade.iur.schema.ADETable;
import org.citydb.ade.iur.schema.SchemaMapper;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citygml4j.ade.iur.model.urt.PublicTransit;
import org.citygml4j.ade.iur.model.urt.Translation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class TranslationImporter implements PublicTransitModuleImporter {
    private final CityGMLImportHelper helper;
    private final SchemaMapper schemaMapper;
    private final PreparedStatement ps;
    private final PublicTransitDataTypeImporter dataTypeImporter;

    private int batchCounter;

    public TranslationImporter(Connection connection, CityGMLImportHelper helper, ImportManager manager) throws CityGMLImportException, SQLException {
        this.helper = helper;
        this.schemaMapper = manager.getSchemaMapper();

        ps = connection.prepareStatement("insert into " +
                helper.getTableNameWithSchema(manager.getSchemaMapper().getTableName(ADETable.TRANSLATION)) + " " +
                "(id, fieldname, fieldvalue, language, language_codespace, recordsubid, tablename, tablename_codespace " +
                "translation, recordid_id) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        dataTypeImporter = manager.getImporter(PublicTransitDataTypeImporter.class);
    }

    public void doImport(Translation translation, long cityObjectGroupId) throws CityGMLImportException, SQLException {
        long objectId = dataTypeImporter.doImport(translation, cityObjectGroupId);
        ps.setLong(1, objectId);

        ps.setString(2, translation.getFieldName());
        ps.setString(3, translation.getFieldValue());

        if (translation.getLanguage() != null && translation.getLanguage().isSetValue()) {
            ps.setString(4, translation.getLanguage().getValue());
            ps.setString(5, translation.getLanguage().getCodeSpace());
        } else {
            ps.setNull(4, Types.VARCHAR);
            ps.setNull(5, Types.VARCHAR);
        }

        ps.setString(6, translation.getRecordSubId());

        if (translation.getTableName() != null && translation.getTableName().isSetValue()) {
            ps.setString(7, translation.getTableName().getValue());
            ps.setString(8, translation.getTableName().getCodeSpace());
        } else {
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.VARCHAR);
        }

        ps.setString(9, translation.getTranslation());

        long recordId = 0;
        if (translation.getRecordId() != null) {
            PublicTransit publicTransit = translation.getRecordId().getObject();
            if (publicTransit != null) {
                recordId = helper.importObject(publicTransit);
                translation.getRecordId().unsetObject();
            } else {
                String href = translation.getRecordId().getHref();
                if (href != null && !href.isEmpty()) {
                    helper.propagateObjectXlink(
                            schemaMapper.getTableName(ADETable.TRANSLATION),
                            objectId, href, "recordid_id");
                }
            }
        }

        if (recordId != 0)
            ps.setLong(10, recordId);
        else
            ps.setNull(10, Types.NULL);

        ps.addBatch();
        if (++batchCounter == helper.getDatabaseAdapter().getMaxBatchSize())
            helper.executeBatch(schemaMapper.getTableName(ADETable.TRANSLATION));
    }

    @Override
    public void executeBatch() throws CityGMLImportException, SQLException {
        if (batchCounter > 0) {
            ps.executeBatch();
            batchCounter = 0;
        }
    }

    @Override
    public void close() throws CityGMLImportException, SQLException {
        ps.close();
    }
}