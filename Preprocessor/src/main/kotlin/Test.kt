fun main() {
    val a: Int = 5 // Первое слагаемое
    val b: Int = 3 /* Второе слагаемое */

    /* Выполняем
    сложение */
    val result: Int = a + b // результат сложения

    var sum: Int = 0;
    // Цикл for
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

    println("Floored pseudo square root: ${calc(sum)}")
}

// Объявление функции
fun calc(sum: Int): Int {
    var root = 0;
    // Цикл while /*
    while (root * root < sum) {
        root += 1
    }

    return root
}
