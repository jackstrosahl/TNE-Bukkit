package net.tnemc.conversion.impl;

import net.tnemc.conversion.ConversionModule;
import net.tnemc.conversion.Converter;
import net.tnemc.conversion.InvalidDatabaseImport;
import net.tnemc.core.TNE;
import net.tnemc.core.common.data.TNEDataManager;
import net.tnemc.core.economy.currency.Currency;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;

public class ZawmgCurrency extends Converter
{
    private File configFile = new File(TNE.instance().getDataFolder(), "../ZawmgDev/config.yml");
    private FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    @Override
    public String name() {
        return "ZawmgCurrency";
    }

    @Override
    public String type() {
        return  "mysql";
    }

    @Override
    public void mysql() throws InvalidDatabaseImport
    {
        String url = config.getString("database.url");

        //remove jdbc:, parse
        URI uri = URI.create(url.substring(5));
        initialize(new TNEDataManager(type(), uri.getHost(),
                uri.getPort() == -1 ? 3306 : uri.getPort(), uri.getPath().substring(1),
                config.getString("database.username"), config.getString("database.password"),
                "", "accounts.db",
                false, false, 60, false));

        open();
        try(Connection connection = db.getConnection();
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery("SELECT uuid.id, uuid_upper_bits, uuid_lower_bits, balance," +
                    " bank FROM currency INNER JOIN uuid ON currency.uuid_id = uuid.id")) {

            final Currency currency = TNE.manager().currencyManager().get(TNE.instance().defaultWorld);
            while(results.next()) {
                int uuidId = results.getInt("uuid.id");
                String id = uuidId == 0 ? results.getString("bank") :
                        (new UUID(results.getLong("uuid_upper_bits"), results.getLong("uuid_lower_bits")).toString());
                ConversionModule.convertedAdd(id,
                        TNE.instance().defaultWorld, currency.name(),
                        new BigDecimal(results.getDouble("balance")));
            }
        } catch(SQLException ignore) {}
        close();
    }
}
