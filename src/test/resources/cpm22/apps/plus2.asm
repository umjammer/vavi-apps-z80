        ORG 0100h         ; .COM program entry point

        LD DE, MSG        ; Address of the message string
        LD C, 9           ; BDOS function 9: Print string at DE
        CALL 0005h        ; Call BDOS
        JP 0000h          ; Jump back to CP/M (WBOOT)

MSG:    DB '2 + 2 = 4$'   ; Dollar-terminated string