package work.fking.rl.stars;


import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelescopeParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+) ?(hours?|minutes?)?");

    private static final String START_OF_TIME = "in the next ";
    private static final String TIME_SEPARATOR = " to ";

    public static StarRegion extractStarRegion(String text) {
        text = text.replace("<br>", " ");
        for (StarRegion region : StarRegion.values()) {
            if (text.contains(region.regionName())) {
                return region;
            }
        }
        return null;
    }

    public static Duration extractEarliestDuration(String text) {
        text = text.replace("<br>", " ");
        int start = text.indexOf(START_OF_TIME) + START_OF_TIME.length();
        int separator = text.lastIndexOf(TIME_SEPARATOR);

        String earliest = text.substring(start, separator).trim();

        return parseDuration(earliest);
    }

    public static Duration extractLatestDuration(String text) {
        text = text.replace("<br>", " ");
        int separator = text.lastIndexOf(TIME_SEPARATOR);

        String latest = text.substring(separator + TIME_SEPARATOR.length()).trim();

        return parseDuration(latest);
    }

    private static Duration parseDuration(String text) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        Duration duration = Duration.ZERO;

        while (matcher.find()) {
            String valueString = matcher.group(1);
            String unit = matcher.group(2);

            if (unit == null) {
                unit = "minutes";
            }
            long value = Long.parseLong(valueString);
            Duration parsedDuration = unit.startsWith("minute") ? Duration.ofMinutes(value) : Duration.ofHours(value);
            duration = duration.plus(parsedDuration);
        }
        return duration;
    }
}
