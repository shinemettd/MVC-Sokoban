public class EnemyModel implements GeneralModel {
    private DatabaseService dbService;
    private Player player;
    private Viewer viewer;
    private String nickName;

    private final int SPACE = 0;
    private final int PLAYER = 1;
    private final int WALL = 2;
    private final int BOX = 3;
    private final int CHECK = 4;
    private final int COIN = 5;

    private String move;
    private int playerPosX;
    private int playerPosY;

    private int[][] map;
    private Levels levelList;

    private int playerCount;
    private int boxesCount;
    private int checksCount;
    private int coinsCount;

    private int totalMoves;
    private int collectedCoins;

    private int[][] checksPos;
    private int[][] coinsPos;
    private Client client;
    private boolean isEnemyCompletedGame;

    public EnemyModel(Viewer viewer) {
        this.viewer = viewer;
        dbService = new DatabaseService();
        player = dbService.getPlayerInfo("Stive");
        levelList = new Levels(client);
        playerPosX = -1;
        playerPosY = -1;
        move = "Down";

    }

    public boolean getIsEnemyCompletedGame() {
        return isEnemyCompletedGame;
    }

    public void setClient(Client client) {
        isEnemyCompletedGame = false;
        levelList = new Levels(client);
        this.client = client;
    }

    public int[][] getDesktop() {
        return map;
    }

    @Override
    public String getNickName() {
        return nickName;
    }

    public void doAction(String action) {
        if (map == null) {
            System.out.println("NO MAP FOUND\n\n");
            return;
        }

        if (action.equals("Left")) {
            move = "Left";
            moveLeft();
        } else if (action.equals("Right")) {
            move = "Right";
            moveRight();
        } else if (action.equals("Up")) {
            move = "Up";
            moveTop();
        } else if (action.equals("Down")) {
            move = "Down";
            moveBot();
        } else if (action.equals("Given up")) {
            viewer.showEnemyGiveUpDialog();
        } else if (action.equals("You have 30 seconds")) {
            viewer.getMyCanvas().setTimer(client, viewer);
            viewer.updateMyCanvas();
            viewer.getModel().setIsPlayerFirstCompletedGame(false);
        } else if (action.equals("complete")) {
            isEnemyCompletedGame = true;
        }

        returnCheck();
        viewer.updateEnemyCanvas();
    }

    public void changeLevel() {
        String data = levelList.getEnemyDataFromServer();

        String[] arrayData = data.split(";");
        nickName = arrayData[0];
        String skin = arrayData[1];
        map = levelList.getEnemyLevelFromServer();
        setPlayer(nickName);
        updateCurrentSkin(skin);

        if (map != null) {
            scanMap();
            viewer.showCanvas("battle");
        }

        totalMoves = 0;
        arrayData = null;
    }

    public String getMove() {
        return move;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public int getCollectedCoins() {
        return collectedCoins;
    }

    public Player setPlayer(String nickname) {
        player = dbService.getPlayerInfo(nickname);
        viewer.updateEnemySkin();
        return player;
    }

    public Player getPlayer() {
        return player;
    }

    public void updateCurrentSkin(String skinType) {
        dbService.updateCurrentSkin(nickName, skinType);
        PlayerSkin skin = null;
        switch (skinType) {
            case "Default Skin":
                skin = new DefaultSkin();
                break;
            case "Santa Skin":
                skin = new SantaSkin();
                break;
            case "Premium Skin":
                skin = new PremiumSkin();
                break;
        }
        player.setCurrentSkin(skin);
    }

    private void scanMap() {
        for (int i = 0; i < map.length - 1; i++) {
            int currentMapLineLength = map[i].length;
            int nextMapLineLength = map[i + 1].length;

            if (nextMapLineLength <= currentMapLineLength) {
                continue;
            }

            int nextMapLineLastElementOfCurrentLine = map[i + 1][map[i].length];
            int nextMapLineLastElement = map[i + 1][map[i + 1].length - 1];

            if ((nextMapLineLastElementOfCurrentLine == 0 || nextMapLineLastElement != 2)) {
                map = null;
                return;
            }
        }

        playerCount = 0;
        boxesCount = 0;
        checksCount = 0;
        totalMoves = 0;
        coinsCount = 0;
        collectedCoins = 0;

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == PLAYER) {
                    playerPosX = j;
                    playerPosY = i;
                    playerCount++;
                } else if (map[i][j] == BOX) {
                    boxesCount++;
                } else if (map[i][j] == CHECK) {
                    checksCount++;
                } else if (map[i][j] == COIN) {
                    coinsCount++;
                }
            }
        }

        if (playerCount != 1 || boxesCount != checksCount || boxesCount == 0 && checksCount == 0) {
            map = null;
            return;
        }

        checksPos = new int[checksCount][2];
        int checksQueue = 0;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == CHECK) {
                    checksPos[checksQueue][0] = i;
                    checksPos[checksQueue][1] = j;
                    checksQueue++;
                }
            }
        }

        coinsPos = new int[coinsCount][2];
        int coinsQueue = 0;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == COIN) {
                    coinsPos[coinsQueue][0] = i;
                    coinsPos[coinsQueue][1] = j;
                    coinsQueue++;
                }
            }
        }
    }

    private boolean isWon() {
        for (int i = 0; i < checksPos.length; i++) {
            int checkPosY = checksPos[i][0];
            int checkPosX = checksPos[i][1];
            if (map[checkPosY][checkPosX] != BOX) {
                return false;
            }
        }
        return true;
    }

    private void returnCheck() {
        for (int i = 0; i < checksPos.length; i++) {
            int checkPosY = checksPos[i][0];
            int checkPosX = checksPos[i][1];
            if (map[checkPosY][checkPosX] == SPACE) {
                map[checkPosY][checkPosX] = CHECK;
                break;
            }
        }

        for (int i = 0; i < coinsPos.length; i++) {
            int coinsPosY = coinsPos[i][0];
            int coinsPosX = coinsPos[i][1];
            boolean coinsValid = coinsPosY != -1 && coinsPosX != -1;

            if (coinsValid && map[coinsPosY][coinsPosX] == SPACE) {
                map[coinsPosY][coinsPosX] = COIN;
                break;
            } else if (coinsValid && map[coinsPosY][coinsPosX] == BOX) {
                coinsPos[i][0] = -1;
                coinsPos[i][1] = -1;
            }
        }
    }

    private void moveLeft() {
        if ((map[playerPosY][playerPosX - 1] == WALL)) {
            System.out.println("Impossible move to the left");
            return;
        }

        if (map[playerPosY][playerPosX - 1] == BOX && !canMoveBoxToLeft()) {
            return;
        }

        if (map[playerPosY][playerPosX - 1] == BOX) {
            if (map[playerPosY][playerPosX - 2] == COIN) {
                collectedCoins++;
            }
            map[playerPosY][playerPosX - 1] = SPACE;

            if (map[playerPosY][playerPosX - 2] == CHECK) {

            }
            map[playerPosY][playerPosX - 2] = BOX;
        }


        map[playerPosY][playerPosX - 1] = PLAYER;
        map[playerPosY][playerPosX] = SPACE;
        playerPosX -= 1;
        totalMoves++;
    }

    private void moveRight() {
        if ((map[playerPosY][playerPosX + 1] == WALL)) {
            return;
        }

        if (map[playerPosY][playerPosX + 1] == BOX && !canMoveBoxToRight()) {
            return;
        }

        if (map[playerPosY][playerPosX + 1] == BOX) {
            if (map[playerPosY][playerPosX + 2] == COIN) {
                collectedCoins++;
            }
            map[playerPosY][playerPosX + 1] = SPACE;

            if (map[playerPosY][playerPosX + 2] == CHECK) {

            }
            map[playerPosY][playerPosX + 2] = BOX;
        }


        map[playerPosY][playerPosX + 1] = PLAYER;
        map[playerPosY][playerPosX] = SPACE;
        playerPosX += 1;
        totalMoves++;
    }

    private void moveTop() {
        if ((map[playerPosY - 1][playerPosX] == WALL)) {
            return;
        }

        if (map[playerPosY - 1][playerPosX] == BOX && !canMoveBoxToTop()) {
            return;
        }

        if (map[playerPosY - 1][playerPosX] == BOX) {
            if (map[playerPosY - 2][playerPosX] == COIN) {
                collectedCoins++;
            }
            map[playerPosY - 1][playerPosX] = SPACE;

            if (map[playerPosY - 2][playerPosX] == CHECK) {

            }
            map[playerPosY - 2][playerPosX] = BOX;
        }


        map[playerPosY - 1][playerPosX] = PLAYER;
        map[playerPosY][playerPosX] = SPACE;
        playerPosY -= 1;
        totalMoves++;
    }

    private void moveBot() {
        if (map[playerPosY + 1][playerPosX] == WALL) {
            return;
        }

        if (map[playerPosY + 1][playerPosX] == BOX && !canMoveBoxToBot()) {
            return;
        }

        if (map[playerPosY + 1][playerPosX] == BOX) {
            if (map[playerPosY + 2][playerPosX] == COIN) {
                collectedCoins++;
            }
            map[playerPosY + 1][playerPosX] = SPACE;

            if (map[playerPosY + 2][playerPosX] == CHECK) {

            }
            map[playerPosY + 2][playerPosX] = BOX;
        }


        map[playerPosY + 1][playerPosX] = PLAYER;
        map[playerPosY][playerPosX] = SPACE;
        playerPosY += 1;
        totalMoves++;
    }

    private boolean canMoveBoxToLeft() {
        if (((map[playerPosY][playerPosX - 2] == WALL) || (map[playerPosY][playerPosX - 2] == BOX)) && (playerPosX - 2 >= 0)) {
            return false;
        }
        return true;
    }

    private boolean canMoveBoxToRight() {
        if (((map[playerPosY][playerPosX + 2] == WALL) || (map[playerPosY][playerPosX + 2] == BOX)) && (playerPosX + 2 < map[playerPosY].length)) {
            return false;
        }
        return true;
    }

    private boolean canMoveBoxToTop() {
        if (((map[playerPosY - 2][playerPosX] == WALL) || (map[playerPosY - 2][playerPosX] == BOX)) && (playerPosY - 2 >= 0)) {
            return false;
        }
        return true;
    }

    private boolean canMoveBoxToBot() {
        if (((map[playerPosY + 2][playerPosX] == WALL) || (map[playerPosY + 2][playerPosX] == BOX)) && (playerPosY + 2 < map[playerPosX].length)) {
            return false;
        }
        return true;
    }
}
