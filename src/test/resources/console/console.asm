        ORG 256         ; CP/M loads .COM programs at address 0x0100 (decimal 256)

        ; --- Print a prompt using BDOS function 9 ---
        LD C, 9         ; C = 9 -> BDOS "Print String" function
        LD DE, MSG      ; DE = address of string (terminated by '$')
        CALL 5          ; Call BDOS function via CP/M jump at address 0005h

        ; --- Read a single character from the user using BDOS function 1 ---
        LD C, 1         ; C = 1 -> BDOS "Read Console Character"
        CALL 5          ; Call BDOS function
        LD E, A         ; Move returned character from A -> E for printing

        ; --- Echo the character back using BDOS function 2 ---
        LD C, 2         ; C = 2 -> BDOS "Write Console Character"
        CALL 5          ; Call BDOS to print character in E

        RET             ; Return (program ends)

; --- Message to display ---
MSG:
        DB 'Type a key: ', 36  ; '$' terminator for BDOS function 9
