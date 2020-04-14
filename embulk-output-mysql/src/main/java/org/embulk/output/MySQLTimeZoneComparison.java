package org.embulk.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MySQLTimeZoneComparison {

    private static final int ONE_HOUR_SEC = 3600;
    private static final int ONE_MIN_SEC = 60;

    private Connection connection;

    private static final Logger logger = LoggerFactory.getLogger(MySQLTimeZoneComparison.class);

    public MySQLTimeZoneComparison(Connection connection)
    {
        this.connection = connection;
    }

    public void compareTimeZone()
            throws SQLException
    {
        TimeZone serverTimeZone = null;
        try {
            serverTimeZone = getServerTimeZone();
        }
        catch (SQLException ex) {
            logger.warn("Can't get server TimeZone.");
            logger.warn(String.format(Locale.ENGLISH, "SQLException raised %s", ex.toString()));
        }

        TimeZone clientTimeZone = TimeZone.getDefault();
        Date today = new Date();
        int clientOffset = clientTimeZone.getRawOffset();

        if (clientTimeZone.inDaylightTime(today)) {
            clientOffset += clientTimeZone.getDSTSavings();
        }

        //
        // Compare offset only. Although I expect to return true, the following code return false,
        //
        // TimeZone tz_jst   = TimeZone.getTimeZone("JST");
        // TimeZone tz_gmt9  = TimeZone.getTimeZone("GMT+9");
        // tz_jst.hasSameRules(tz_gmt9) // return false.
        //
        if (clientOffset != serverTimeZone.getRawOffset()) {
            logger.warn(String.format(Locale.ENGLISH,
                    "The client timezone(%s) is different from the server timezone(%s). The plugin will store wrong datetime values.",
                    clientTimeZone.getID(), serverTimeZone.getID()));
            logger.warn(String.format(Locale.ENGLISH,
                    "You may need to set options `useLegacyDatetimeCode` and `serverTimezone`"));
            logger.warn(String.format(Locale.ENGLISH,
                    "Example. `options: { useLegacyDatetimeCode: false, serverTimezone: UTC }`"));
        }
        logger.warn(String.format(Locale.ENGLISH, "The plugin will set `useLegacyDatetimeCode=false` by default in future."));
    }

    private TimeZone getServerTimeZone()
            throws SQLException
    {
        //
        // First, I used `@@system_time_zone`. but It return non Time Zone Abbreviations name on a specific platform.
        // So, This method calculate GMT offset with query.
        //
        String query = "select TIME_TO_SEC(timediff(now(),utc_timestamp()));";
        Statement stmt = connection.createStatement();

        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int offsetSeconds = rs.getInt(1);
                return fromGMTOffsetSeconds(offsetSeconds);
            }
            throw new SQLException(String.format(Locale.ENGLISH,
                    "The timezone comparison query(%s) doesn't return the result.",query));
        }
        finally {
            stmt.close();
        }
    }

    private TimeZone fromGMTOffsetSeconds(int offsetSeconds)
    {
        if (offsetSeconds == 0) {
            return TimeZone.getTimeZone("UTC");
        }

        String sign = offsetSeconds > 0 ? "+" : "-";
        int absOffsetSec = Math.abs(offsetSeconds);
        int tzHour = absOffsetSec / ONE_HOUR_SEC;
        int tzMin = absOffsetSec % ONE_HOUR_SEC / ONE_MIN_SEC;
        String tzName = String.format(Locale.ENGLISH, "GMT%s%02d:%02d", sign, tzHour, tzMin);
        return TimeZone.getTimeZone(tzName);
    }


}
