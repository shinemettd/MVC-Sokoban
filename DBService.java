import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DBService {
    private final String coinsPath = "db/passed_levels.csv";
    private final String skinsPath = "db/available_skins.csv";
    private final String totalCoinsPath = "db/total_coins.csv";

    public DBService() {}

    public Player getPlayerInfo(String nickname) {
        createMissingFiles();

        HashMap<Integer, Integer> coinsOnLevels = readCoinsData(nickname);
        ArrayList<String> availableSkins = readSkinsData(nickname);
        int totalCoins = readTotalCoinsData(nickname);

        return new Player(nickname, coinsOnLevels, availableSkins, totalCoins);
    }

    public void writeCoins(String nickname, int level, int coins) {
        try {
            boolean fileExists = Files.exists(Paths.get(coinsPath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(coinsPath, true));

            if(!fileExists) {
                writer.append("Nickname;Level;Coins");
                writer.newLine();
            }

            String fileContent = new String(Files.readAllBytes(Paths.get(coinsPath)));

            Pattern pattern = Pattern.compile(nickname + ";" + level + ";(\\d+)");
            Matcher matcher = pattern.matcher(fileContent);

            if (matcher.find()) {
                int currentCoins = Integer.parseInt(matcher.group(1));

                if (coins > currentCoins) {
                    fileContent = fileContent.replaceAll(nickname + ";" + level + ";\\d+", nickname + ";" + level + ";" + coins);
                    Files.write(Paths.get(coinsPath), fileContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                    writeTotalCoins(nickname, coins - currentCoins);
                }

            } else {
                writer.append(nickname + ";" + level + ";" + coins);
                writer.newLine();
                writer.close();

                writeTotalCoins(nickname, coins);
            }

        } catch(IOException e) {
            System.out.println(e);
        }
    }

    public void addSkin(String nickname, String skin) {
        try {
            boolean fileExists = Files.exists(Paths.get(skinsPath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(skinsPath, true));

            if(!fileExists) {
                writer.append("Nickname;Skin");
                writer.newLine();
            }

            writer.append(nickname + ";" + skin);
            writer.newLine();
            writer.close();

        } catch(IOException e) {
            System.out.println(e);
        }
    }

    public void readDataFromCSV() {
        try (BufferedReader reader = new BufferedReader(new FileReader("passed_levels.csv"))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(";");
                String nickname = data[0];
                int level = Integer.parseInt(data[1]);
                int coins = Integer.parseInt(data[2]);
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void writeTotalCoins(String nickname, int value) {
        try {
            boolean fileExists = Files.exists(Paths.get(totalCoinsPath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(totalCoinsPath, true));

            if (!fileExists) {
                writer.append("Nickname;Coins");
                writer.newLine();
            }

            String fileContent = new String(Files.readAllBytes(Paths.get(totalCoinsPath)));

            Pattern pattern = Pattern.compile(nickname +  ";(\\d+)");
            Matcher matcher = pattern.matcher(fileContent);

            if(matcher.find()) {
                int currentCoins = Integer.parseInt(matcher.group(1));
                fileContent = fileContent.replaceAll(nickname + ";\\d+", nickname + ";" + (value + currentCoins));
                Files.write(Paths.get(totalCoinsPath), fileContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                writer.append(nickname + ";" + value);
                writer.newLine();
                writer.close();
            }

        } catch(IOException e) {
            System.out.println(e);
        }
    }

    private void createMissingFiles() {
        createFileIfNotExists(coinsPath, "Nickname;Level;Coins");
        createFileIfNotExists(skinsPath, "Nickname;Skin");
        createFileIfNotExists(totalCoinsPath, "Nickname;Coins");
    }

    private void createFileIfNotExists(String filePath, String header) {
        try {
            boolean fileExists = Files.exists(Paths.get(filePath));

            if (!fileExists) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    writer.append(header);
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private HashMap<Integer, Integer> readCoinsData(String nickname) {
        HashMap<Integer, Integer> coinsOnLevels = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(coinsPath))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(";");
                String nicknameFromDB = data[0];

                if (nickname.equals(nicknameFromDB)) {
                    int level = Integer.parseInt(data[1]);
                    int coins = Integer.parseInt(data[2]);
                    coinsOnLevels.put(level, coins);
                }
            }

        } catch (IOException e) {
            System.out.println(e);
        }

        return coinsOnLevels;
    }

    private ArrayList<String> readSkinsData(String nickname) {
        ArrayList<String> availableSkins = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(skinsPath))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(";");
                String nicknameFromDB = data[0];

                if (nickname.equals(nicknameFromDB)) {
                    availableSkins.add(data[1]);
                }
            }

        } catch (IOException e) {
            System.out.println(e);
        }

        return availableSkins;
    }

    private int readTotalCoinsData(String nickname) {
      int totalCoins = 0;

      try (BufferedReader reader = new BufferedReader(new FileReader(totalCoinsPath))) {
          reader.readLine();
          String line;

          while ((line = reader.readLine()) != null) {
              String[] data = line.split(";");
              String nicknameFromDB = data[0];

              if (nickname.equals(nicknameFromDB)) {
                  totalCoins = Integer.parseInt(data[1]);
                  break;
              }
          }

      } catch (IOException e) {
          System.out.println(e);
      }

      return totalCoins;
    }
}
