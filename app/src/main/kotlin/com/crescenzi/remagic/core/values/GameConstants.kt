/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.core.values

/**
 * Game constants
 */
object GameConstants {
    const val MAGICIAN_FRAME_SIDE = 240
    const val MAGICIAN_SPEED = 60f

    const val TARGET_SPAWN_RATE = 1500L
    const val TARGET_SPEED = 10f

    const val NOTIFICATION_RATE = 5
    const val NOTIFICATION_LLM_PROMPT = "Crea un breve messaggio di incoraggiamento per un utente da usare come corpo di notifica. Deve essere conciso, positivo e motivante, tipo: 'Complimenti, stai andando alla grande!'. L'output deve essere pulito e pronto all'uso."
}
