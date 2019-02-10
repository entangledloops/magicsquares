import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Stephen on 4/5/2015.
 * Updated 12/3/2018.
 */
public class MagicSquare extends JFrame
{
  final boolean      enableGui         = true; // show search in gui
  final boolean      allowDupes        = false; // allow duplicate numbers?
  final int          size              = 10; // square will be size*size
  final int          power             = 1; // 2 = bimagic, 3 = trimagic, etc.
  final long         seed              = 3493;
  final int          upperBound        = size*size*4;
  final int          lowerBound        = 1;
  final long         improveIterations = 10; // number of resample attempts per cell
  final long         improveRounds     = 1000; // number of resample iterations per square
  final long         minUpdates        = 1; // min number of updates in a round needed to keep search going
  final long         maxRestarts       = 1000; // number of restart attempts per solve()
  final long[][]     square            = new long[size][size];
  final Random       random            = new Random(seed);
  final TextArea[][] squareText        = new TextArea[size][size];
  final Font         font              = new Font("Monospace", Font.BOLD, 30);

  // these cached for performance
  long[] r = new long[size]; // row sums
  long[] c = new long[size]; // col sums
  long   d1, d2; // diagonal sums
  long distance; // current distance from solution (sum of diffs between each row, col, and diagonal)

  public MagicSquare()
  {
    super();
    if (enableGui) createGui();
  }

  private void createGui()
  {
    final JPanel panel = new JPanel(new GridLayout(size, size));
    final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        squareText[i][j] = new TextArea("");
        squareText[i][j].setFont(font);
        panel.add(squareText[i][j]);
      }
    }

    setSize(1024, 768);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
    setVisible(true);
    add(panel);
  }

  public void updateGui(long[][] square)
  {
    if (!enableGui) return;
    setTitle("distance: " + distance);
    for (int i = 0; i < size; i++)
      for (int j = 0; j < size; j++)
        squareText[i][j].setText(square[i][j]+"");
  }

  public void solve()
  {
    final long startTime = System.nanoTime();
    System.out.println("seed: " + seed + "\ndistance");

    long restarts = 0;
    while (restarts++ < maxRestarts)
    {
      for (int r = 0; r < size; ++r) {
        for (int c = 0; c < size; ++c) {
          square[r][c] = next();
        }
      }
      distance = goalDistance(square);
      updateGui(square);

      int updateCount;
      do {
        updateCount = 0;
        for (int rounds = 0; rounds < improveRounds; ++rounds) {
          for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
              if (improve(square, i, j)) {
                ++updateCount;
                if (0 == distance) break;
              }
            }
            if (0 == distance) break;
          }
          if (0 == distance || updateCount < minUpdates) break;
        }
      } while (0 != distance && updateCount >= minUpdates);

      System.out.println(distance);

      if (0 == distance) {
        printSquare(square);
        break;
      }
    }

    final long elapsed = System.nanoTime() - startTime;
    System.out.println("restarts: " + (restarts-1) + ", elapsed: " + (elapsed/1000000000.) + "s");
  }

  // pick next number, avoiding duplicates
  public long next()
  {
    while (true)
    {
      long next = random.nextInt(upperBound - lowerBound) + lowerBound;
      if (allowDupes) return next;
      boolean found = false;
      for (int i = 0; i < size; ++i) {
        for (int j = 0; j < size; ++j) {
          if (square[i][j] == next) {
            found = true;
            break;
          }
        }
        if (found) break;
      }
      if (!found) return next;
    }
  }

  public boolean improve(long square[][], int r, int c)
  {
    boolean updated = false;
    long best = square[r][c];

    for (int i = 0; i < improveIterations; ++i)
    {
      square[r][c] = next();
      long distance = goalDistance(square);
      if (distance < this.distance)
      {
        updated = true;
        best = square[r][c];
        this.distance = distance;
        updateGui(square);
        break;
      }
    }

    square[r][c] = best;
    return updated;
  }

  public void printSquare(long[][] square)
  {
    for (int i = 0; i < size; i++)
    {
      System.out.print("{");
      for (int j = 0; j < size; j++) {
        System.out.print(square[i][j]);
        if (j != size -1) System.out.print(", ");
      }
      System.out.print("}");
      if (i != size -1) System.out.println(",");
    }
    System.out.println("\ndistance: " + distance);
  }

  public long dirtyPow(long value, int exp)
  {
    if (0 == exp) return 1;
    else if (1 == exp) return value;
    else if (2 == exp) return value*value;
    else if (3 == exp) return value*value*value;
    else return new BigInteger(Long.toString(value)).pow(exp).longValue();
  }

  public long goalDistance(long[][] square)
  {
    Arrays.fill(r, 0);
    Arrays.fill(c, 0);
    d1 = d2 = 0;
    long distance = 0;

    for (int i = 0; i < size; i++)
      for (int j = 0; j < size; j++)
      {
        final long value = square[i][j] + (power > 1 ? dirtyPow(square[i][j], power) : 0);
        r[i] += value;
        c[j] += value;
        if (i == j) d1 += value;
        if ((i + j) == (size - 1)) d2 += value;
      }

    for (int i = 0; i < size; i++) {
      if (i >= 1) {
        distance += Math.abs(r[i]-r[i-1]);
        distance += Math.abs(c[i]-c[i-1]);
      }
      distance += Math.abs(r[i]-c[i]);
      distance += Math.abs(r[i]-d1);
      distance += Math.abs(r[i]-d2);
      distance += Math.abs(c[i]-d1);
      distance += Math.abs(c[i]-d2);
    }

    return distance;
  }

  public static void main(String[] args)
  {
    MagicSquare magicSquare = new MagicSquare();
    magicSquare.solve();
  }
}