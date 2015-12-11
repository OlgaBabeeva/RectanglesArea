import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;

public final class RectanglesArea {

    public static void main(final String[] args) throws IOException {
        if (args.length != 2) {
            throw new RuntimeException("Wrong number of arguments");
        }
        final InputStream is = correctInput(args);
        final OutputStream os = correctOutput(args);
        if (is == null || os == null) {
            return;
        }
        final List<Rectangle> rectangleList = Reader.readInput(is);
        final RectArea ra = new RectArea(rectangleList);
        final long area = ra.execute();
        Writer.writeOutput(os, area);
    }

    public static InputStream correctInput(final String[] args) {
        InputStream targetStream = null;
        try {
            final File initialFile = new File(Paths.get(args[0]).toString());
            targetStream = new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            System.out.println("There is no input file");
        }
        return targetStream;
    }

    public static OutputStream correctOutput(final String[] args) {
        OutputStream targetStream = null;
        try {
            final File initialFile = new File(Paths.get(args[1]).toString());
            targetStream = new FileOutputStream(initialFile);
        } catch (FileNotFoundException e) {
            System.out.println("There is no output file");
        }
        return targetStream;
    }

}

final class Reader {
    private static final int MAX_COORD = 10000;

    private Reader() {
    }

    public static List<Rectangle> readInput(final InputStream is) throws IOException {
        final List<Rectangle> rectangleList = new ArrayList<>();

        final BufferedReader scanner = new BufferedReader(new InputStreamReader(is));

        String line;
        int k = 0;
        while ((line = scanner.readLine()) != null) {
            k++;
            final String[] result = line.split(" ");
            if (result.length != 4) {
                throw new IllegalArgumentException("There are not 4 numbers in " + k + " line");
            }
            for (final String str : result) {
                if (!isNumeric(str)) {
                    throw new IllegalArgumentException("There is not a number in " + k + " line");
                }
            }
            final int[] coords = new int[4];
            for (int i = 0; i < 4; i++) {
                coords[i] = Integer.parseInt(result[i]);
                if (Math.abs(coords[i]) > MAX_COORD) {
                    throw new IllegalArgumentException(
                            "There is a number less than -10 000 or greater than 10 000 in " + k + " line");
                }
            }

            sortCoords(coords);

            rectangleList.add(new Rectangle(new Point(coords[0], coords[1]), new Point(coords[2], coords[3])));

            if (k > 100) {
                throw new IllegalArgumentException("There is more than 100 lines in input file");
            }
        }
        scanner.close();
        if (rectangleList.size() == 0) throw new IllegalArgumentException("Input file is empty");
        return rectangleList;
    }

    private static int[] sortCoords(final int[] array) {
        for (int i = 0; i < 2; i++) {
            if (array[i] > array[i + 2]) {
                final int temp = array[i];
                array[i] = array[i + 2];
                array[i + 2] = temp;
            }
        }
        return array;
    }

    public static boolean isNumeric(final String str) {
        final NumberFormat formatter = NumberFormat.getInstance();
        final ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }
}

final class Writer {
    private Writer() {
    }

    public static void writeOutput(final OutputStream os, final long area) throws IOException {
        final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
        out.write(String.valueOf(area));
        out.close();
    }
}


class RectArea {
    final PriorityQueue<Event> queue;
    final SweepLineCounter counter;

    RectArea(final List<Rectangle> rectangles) {
        queue = new PriorityQueue<>();
        for (final Rectangle rectangle : rectangles) {
            queue.add(new Event(rectangle.leftBottom().x, false, rectangle));
            queue.add(new Event(rectangle.rightTop().x, true, rectangle));
        }

        counter = new SqueezeArrayCounter(rectangles);
    }

    public long execute() {
        int oldX = 0;
        long area = 0;
        while (queue.size() > 0) {
            final Event event = queue.poll();
            area += (event.key() - oldX) * counter.getTotalLength();
            counter.add(event);
            oldX = event.key();
        }
        return area;
    }
}

class Event implements Comparable<Event> {
    private final int key;
    private final boolean type;
    private final Rectangle rectangle;

    Event(final int key, final boolean type, final Rectangle rectangle) {
        this.key = key;
        this.rectangle = rectangle;
        this.type = type;
    }

    public int key() {
        return key;
    }

    public boolean type() {
        return type;
    }

    public Rectangle rectangle() {
        return rectangle;
    }

    @Override
    public int compareTo(final Event event) {
        return (int) Math.signum(this.key - event.key);
    }

}


class Rectangle {
    private final Point leftBottom;
    private final Point rightTop;

    Rectangle(final Point leftBottom, final Point rightTop) {
        this.leftBottom = leftBottom;
        this.rightTop = rightTop;
    }

    public Point leftBottom() {
        return leftBottom;
    }

    public Point rightTop() {
        return rightTop;
    }
}


interface SweepLineCounter {
    void add(Event c);

    int getTotalLength();
}


class NaiveArrayCounter implements SweepLineCounter {
    private final int[] coordinates;
    private static final int MAX_COORD = 10000;
    private static final int TOTAL_LENGHT = 2 * MAX_COORD + 1;

    NaiveArrayCounter() {
        coordinates = new int[TOTAL_LENGHT];
    }

    @Override
    public int getTotalLength() {
        int length = 0;
        for (final int coordinate : coordinates) {
            if (coordinate != 0) {
                length++;
            }
        }
        return length;
    }

    @Override
    public void add(final Event event) {
        for (int i = event.rectangle().leftBottom().y; i < event.rectangle().rightTop().y; i++) {
            coordinates[i + MAX_COORD] += event.type() ? -1 : 1;
        }
    }
}

class SqueezeArrayCounter implements SweepLineCounter {
    private final List<WeightedArray> weightedArrays;

    SqueezeArrayCounter(final List<Rectangle> RectangleList) {
        final SortedSet<Integer> ordinates = new TreeSet<>();
        weightedArrays = new ArrayList<>();

        for (final Rectangle rectangle : RectangleList) {
            ordinates.add(rectangle.leftBottom().y);
            ordinates.add(rectangle.rightTop().y);
        }

        final Integer[] ordinatesArray = ordinates.toArray(new Integer[ordinates.size()]);

        for (int i = 0; i + 1 < ordinatesArray.length; i++) {
            weightedArrays.add(new WeightedArray(ordinatesArray[i].intValue(), ordinatesArray[i + 1].intValue()));
        }
    }

    @Override
    public int getTotalLength() {
        int length = 0;
        for (final WeightedArray array : weightedArrays) {
            if (array.weight > 0) {
                length += array.end - array.start;
            }
        }
        return length;
    }

    @Override
    public void add(final Event event) {
        final int startY = event.rectangle().leftBottom().y;
        final int endY = event.rectangle().rightTop().y;

        if (!event.type()) {
            weightedArrays.stream().filter(array -> array.start >= startY && array.end <= endY).forEach(array -> {
                array.weight++;
            });
        } else {
            weightedArrays.stream().filter(array -> array.start >= startY && array.end <= endY).forEach(array -> {
                array.weight--;
            });
        }
    }

    private static class WeightedArray {
        int start;
        int end;
        int weight;

        WeightedArray(final int startY, final int endY) {
            start = startY;
            end = endY;
            weight = 0;
        }
    }
}