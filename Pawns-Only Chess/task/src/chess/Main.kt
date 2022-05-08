package chess

import kotlin.math.absoluteValue

/**
 * Тип пешки: Белая или черная, устанавливается также строка  по умолчанию для расставления пешек 2 и 7
 */
enum class PawnType ( val initialRow: Int) {WHITE( 2), BLACK(7)}
enum class GameStatus {PLAYER1WINS, PLAYER2WINS, STOPED, STALEMATE, IN_PROCESS}

class Pawn (var x: Int, var y: Int, val color: PawnType) {
    var inGame = true
    fun isWhite(): Boolean = color == PawnType.WHITE
}

class Player {
    var pawns: List<Pawn>
    val name:String
    val type: PawnType

    constructor(name: String, type: PawnType) {
        this.name = name
        this.type = type
        var i = 0
        this.pawns = List(8) { Pawn(++i, type.initialRow, type) }
    }
}

class Chess (player1Name: String, player2Name: String){
    val player1 = Player(player1Name, PawnType.WHITE)
    val player2 = Player(player2Name, PawnType.BLACK)
    var activePlayer = player1
    var gameStatus: GameStatus = GameStatus.IN_PROCESS
    lateinit var lastMovedPawn: Pawn

    fun startGame() =  makePlayerMove()

    private fun makePlayerMove() {

        println("${activePlayer.name}'s turn:")
        val move = readln()
        // если exit прекращаем игру
        if(move == "exit") {
            gameStatus = GameStatus.STOPED
            println("Bye!")
            return
        }

        try {
            // пробуем сделать ход по введнным пользователем данным
            makeMove(move)
        } catch (e: Exception) {
            // если ввел фигню даем ввести еще раз
            println(e.message)
            makePlayerMove()
            return
        }
        changeActivePlayer()
        printBoard()
        updateGameStatus()
        when (gameStatus) {
            GameStatus.PLAYER1WINS -> println("White Wins!\nBye!")
            GameStatus.PLAYER2WINS -> println("Black Wins!\nBye!")
            GameStatus.STALEMATE -> println("Stalemate!\nBye!")
            GameStatus.IN_PROCESS -> {
                makePlayerMove()
            }
        }

    }

    /**
     * проверяет и обновляет данные о том ни выииграл ли кто-то или ни случилась ли ничья
     */
    private fun updateGameStatus() {
        // если пешка достигла конца поля игра окончеена
        for (paw in (player1.pawns + player2.pawns)) {
            if(paw.y == 1 || paw.y == 8) {
                gameStatus = if(paw.isWhite()) GameStatus.PLAYER1WINS else GameStatus.PLAYER2WINS
                return

            }
        }
        //  если у игрока не осталось пешек он проиграл
        if (player1.pawns.filter { it.inGame == true }.isEmpty()) {
            gameStatus = GameStatus.PLAYER2WINS
            return
        }
        if (player2.pawns.filter { it.inGame == true }.isEmpty()) {
            gameStatus = GameStatus.PLAYER1WINS
            return
        }

        // Если у игрока к которому перешел ход нет ходов - ничья
        var isDraw = true
        Loop@ for (pawn in activePlayer.pawns) {
            if(!pawn.inGame) continue
            val checkingXList  = mutableListOf<Int>()
            (-1..0).forEach { if ((pawn.x + it) in (1..8)) checkingXList.add(pawn.x + it) }
            val checkingY = if (pawn.isWhite()) pawn.y + 1 else pawn.y - 1


            for (checkingX in checkingXList) {
                try {
                    checkMoveValidity(pawn.x, pawn.y, checkingX, checkingY)
                    isDraw = false
                    break@Loop

                }  catch (e: Exception) {}
            }

        }
        if (isDraw) gameStatus = GameStatus.STALEMATE
    }



    /**
     * преобразует строку вида a2a4 в координты [1,2,1,4]
     */
    private fun convertStringMoveToInt(move: String):MutableList<Int> {
        val l = mutableListOf<Int>()
        check(move.matches(Regex("[a-h][1-8][a-h][1-8]"))) {"Invalid Input"}

        move.forEach { l.add(if (it.isDigit()) it.digitToInt() else (it.code - 'a'.code + 1))}
        return  l
    }

    private fun checkMoveValidity (x1: Int, y1: Int, x2: Int, y2: Int) {

        // для полноты проверим что все переменные от 1 до 8
        val wrongInput = "Invalid Input"
        check(x1 in 1..8 && x2 in 1..8 && y1 in 1..8 && y2 in 1..8 ) {wrongInput}

        // проверим изначальные координаты на наличие пешки цвета активного игрока
        val fromPawn = getPawn(x1,y1)
        val chars = ('a'..'h').toList()
        check(fromPawn != null && fromPawn.color == activePlayer.type) {"No ${activePlayer.type.name.lowercase()} pawn at ${chars[x1-1]}$y1"}

        // проверка что смещение по x не более 1
        check ((x1 - x2).absoluteValue <= 1) {wrongInput}
        val toPawn = getPawn(x2,y2)
        // если шаг вперед
        if(x1 == x2) {

            // проверим что ход на 1 или 2 клетки вперед
            if(fromPawn.isWhite()) {
                check (y2 - y1 == 1 || y1 == PawnType.WHITE.initialRow && y2 - y1 == 2) {wrongInput}
            } else {
                check (y1 - y2 == 1 || y1 == PawnType.BLACK.initialRow && y1 - y2 == 2) {wrongInput}
            }
            // клетка впереди пуста
            check(toPawn == null) {wrongInput}

        }

        //  если ход под диагонали то клетка должна быть либо пустой и позади стоит пешка противника которой  делали предыдущий ход либо занята пешкой противника
        if (x1 != x2) {
            val backPawn = getPawn(x2, if(fromPawn.isWhite()) y2-1 else y2+1)
            check(
                // клетка пуста и позади пешка противника которая ходила последней
                toPawn == null && backPawn!= null && backPawn.color != fromPawn.color && ::lastMovedPawn.isInitialized && backPawn == lastMovedPawn ||
                // клетка не пуста и на ней  пешка противника
                toPawn != null && toPawn.color != fromPawn.color
            ) {wrongInput}
        }
    }

    /**
     * Принимает на вход строку вида "a2a4" проверяет валидноcmь хода, перемещает пешку, удаляет убитых
     */
    private fun makeMove(move: String) {
        // конвертем a2a4 в [1, 2, 1, 4]
        val (x1, y1, x2, y2) = convertStringMoveToInt(move)
        // проверим можно ли туда ходить, если нет Exception
        checkMoveValidity(x1, y1, x2, y2)
        // пешка которая ходит
        val fromPawn = getPawn(x1, y1)

        // пешка на которую ходят или null если там пусто
        val toPawn = getPawn(x2, y2)

        // деакетивирует убитых пешек
        toPawn?.inGame = false

        // если ход по диагоали  деактивирует убитую пешку позади
        if (toPawn == null && x2 !=x1) {
            val backPawn = getPawn(x2, if(fromPawn!!.isWhite()) y2-1 else y2+1)
            backPawn?.inGame = false
        }
        // переместим пешку
        fromPawn?.x = x2
        fromPawn?.y = y2

        // сохраним пешку ходившую последней для валидации En Passant
        if (fromPawn != null) {
            lastMovedPawn = fromPawn
        }
    }

    /**
     * Переводит управление к другому игроку
     */
    fun changeActivePlayer() {
        activePlayer = if (activePlayer == player1) player2 else player1
    }
    /**
     * Выводит в консоль шахматну доску с пешками на ней
     *   +---+---+---+---+---+---+---+---+
     * 8 |   |   |   |   |   |   |   |   |
     *   +---+---+---+---+---+---+---+---+
     * 7 | B | B | B | B | B | B | B | B |
     *   +---+---+---+---+---+---+---+---+
     * 6 |   |   |   |   |   |   |   |   |
     *   +---+---+---+---+---+---+---+---+
     * 5 |   |   |   |   |   |   |   |   |
     *   +---+---+---+---+---+---+---+---+
     * 4 |   |   |   |   |   |   |   |   |
     *   +---+---+---+---+---+---+---+---+
     * 3 |   |   |   |   |   |   |   |   |
     *   +---+---+---+---+---+---+---+---+
     * 2 | W | W | W | W | W | W | W | W |
     *   +---+---+---+---+---+---+---+---+
     * 1 |   |   |   |   |   |   |   |   |
     *   +---+---+---+---+---+---+---+---+
     *   a   b   c   d   e   f   g   h
     */
    fun printBoard() {
        val devider = "  +---+---+---+---+---+---+---+---+"
        println(devider)
        for (y in 8 downTo 1) {
            // заголовок строки цифра
            print ("$y |")
            for (x in 1  .. 8) {
                // проверяем стоит ли пешка на квадрате
                val pawn = getPawn(x,y)
                val letter =  if(pawn != null)  pawn.color.name[0] else ' '
                print (" $letter |")
            }
            println("\n$devider")
        }
        // заголовки столбцов a .. h
        print("  ")
        for (c in 'a'..'h') print("  $c ")
        println()
    }

    /**
     *  Возвращает пешку или null если клетка пуста
     */
    private fun getPawn(x: Int, y:Int): Pawn? {
        for (pawn in (player1.pawns + player2.pawns)) {
            if(pawn.x == x && pawn.y == y && pawn.inGame) return pawn
        }
        return null
    }
}
fun main() {
    println(" Pawns-Only Chess")

    println("First Player's name:")
    val pl1 = readln()
    println("Second Player's name:")
    val pl2 = readln()

    val chess = Chess(pl1, pl2)
    chess.printBoard()
    chess.startGame()


}