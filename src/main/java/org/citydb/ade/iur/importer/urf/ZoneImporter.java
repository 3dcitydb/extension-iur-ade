/*
 * iur-ade-citydb - i-Urban Revitalization ADE extension for the 3DCityDB
 * https://github.com/3dcitydb/iur-ade-citydb
 *
 * iur-ade-citydb is part of the 3D City Database project
 *
 * Copyright 2019-2020 virtualcitySYSTEMS GmbH
 * https://www.virtualcitysystems.de/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citydb.ade.iur.importer.urf;

import org.citydb.ade.importer.CityGMLImportHelper;
import org.citydb.ade.importer.ForeignKeys;
import org.citydb.ade.iur.importer.ImportManager;
import org.citydb.ade.iur.schema.ADETable;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citygml4j.ade.iur.model.urf.Zone;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class ZoneImporter implements UrbanFunctionModuleImporter {
    private final CityGMLImportHelper helper;
    private final PreparedStatement ps;
    private final UrbanFunctionImporter urbanFunctionImporter;

    private int batchCounter;

    public ZoneImporter(Connection connection, CityGMLImportHelper helper, ImportManager manager) throws CityGMLImportException, SQLException {
        this.helper = helper;

        ps = connection.prepareStatement("insert into " +
                helper.getTableNameWithSchema(manager.getSchemaMapper().getTableName(ADETable.ZONE)) + " " +
                "(id, objectclass_id, areaapplied, finalpublicationdate) " +
                "values (?, ?, ?, ?)");

        urbanFunctionImporter = manager.getImporter(UrbanFunctionImporter.class);
    }

    public void doImport(Zone zone, long objectId, AbstractObjectType<?> objectType, ForeignKeys foreignKeys) throws CityGMLImportException, SQLException {
        urbanFunctionImporter.doImport(zone, objectId, objectType, foreignKeys);

        ps.setLong(1, objectId);
        ps.setInt(2, objectType.getObjectClassId());

        ps.setString(3, zone.getAreaApplied());

        if (zone.getFinalPublicationDate() != null)
            ps.setDate(4, Date.valueOf(zone.getFinalPublicationDate()));
        else
            ps.setNull(4, Types.DATE);

        ps.addBatch();
        if (++batchCounter == helper.getDatabaseAdapter().getMaxBatchSize())
            helper.executeBatch(objectType);
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