fun main() {
    val a: Int = 5 // Первое слагаемое
    val b: Int = 3 /* Второе слагаемое */

    /* Выполняем
    сложение */
    val result: Int = a + b // результат сложения

    var sum: Int = 0;
    for (i in 1..result) {
       sum += i;
    }

    // Проверка
    if (sum % 2 == 0) {
        //Вывод/
        println("Even") // Чет
    } else {
        println("Odd") /**нечет*/
    }
}
