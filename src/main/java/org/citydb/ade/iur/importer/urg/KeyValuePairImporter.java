package org.citydb.ade.iur.importer.urg;

import org.citydb.ade.importer.CityGMLImportHelper;
import org.citydb.ade.iur.importer.ImportManager;
import org.citydb.ade.iur.schema.ADESequence;
import org.citydb.ade.iur.schema.ADETable;
import org.citydb.ade.iur.schema.SchemaMapper;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citygml4j.ade.iur.model.urg.KeyValuePair;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class KeyValuePairImporter implements StatisticalGridModuleImporter {
    private final CityGMLImportHelper helper;
    private final SchemaMapper schemaMapper;
    private final PreparedStatement ps;

    private int batchCounter;

    public KeyValuePairImporter(Connection connection, CityGMLImportHelper helper, ImportManager manager) throws CityGMLImportException, SQLException {
        this.helper = helper;
        this.schemaMapper = manager.getSchemaMapper();

        ps = connection.prepareStatement("insert into " +
                helper.getTableNameWithSchema(schemaMapper.getTableName(ADETable.KEYVALUEPAIR)) + " " +
                "(id, statisticalg_genericvalue_id, key, key_codespace, codevalue, codevalue_codespace, datevalue, " +
                "doublevalue, intvalue, measuredvalue, measuredvalue_uom, stringvalue, urivalue) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    public long doImport(KeyValuePair keyValuePair, long parentId) throws CityGMLImportException, SQLException {
        long objectId = helper.getNextSequenceValue(schemaMapper.getSequenceName(ADESequence.KEYVALUEPAIR_SEQ));
        ps.setLong(1, objectId);

        ps.setLong(2, parentId);

        if (keyValuePair.getKey() != null && keyValuePair.getKey().isSetValue()) {
            ps.setString(3, keyValuePair.getKey().getValue());
            ps.setString(4, keyValuePair.getKey().getCodeSpace());
        } else {
            ps.setNull(3, Types.VARCHAR);
            ps.setNull(4, Types.VARCHAR);
        }

        if (keyValuePair.isSetCodeValue() && keyValuePair.getCodeValue().isSetValue()) {
            ps.setString(5, keyValuePair.getCodeValue().getValue());
            ps.setString(6, keyValuePair.getCodeValue().getCodeSpace());
        } else {
            ps.setNull(5, Types.VARCHAR);
            ps.setNull(6, Types.VARCHAR);
        }

        if (keyValuePair.isSetDateValue())
            ps.setDate(7, Date.valueOf(keyValuePair.getDateValue()));
        else
            ps.setNull(7, Types.DATE);

        if (keyValuePair.isSetDoubleValue())
            ps.setDouble(8, keyValuePair.getDoubleValue());
        else
            ps.setNull(8, Types.DOUBLE);

        if (keyValuePair.isSetIntValue())
            ps.setInt(9, keyValuePair.getIntValue());
        else
            ps.setNull(9, Types.INTEGER);

        if (keyValuePair.isSetMeasuredValue() && keyValuePair.getMeasuredValue().isSetValue()) {
            ps.setDouble(10, keyValuePair.getMeasuredValue().getValue());
            ps.setString(11, keyValuePair.getMeasuredValue().getUom());
        } else {
            ps.setNull(10, Types.DOUBLE);
            ps.setNull(11, Types.VARCHAR);
        }

        ps.setString(12, keyValuePair.getStringValue());
        ps.setString(13, keyValuePair.getUriValue());

        ps.addBatch();
        if (++batchCounter == helper.getDatabaseAdapter().getMaxBatchSize())
            helper.executeBatch(schemaMapper.getTableName(ADETable.KEYVALUEPAIR));

        return objectId;
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