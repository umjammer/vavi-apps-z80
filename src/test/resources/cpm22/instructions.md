## Not ideal, but working approach of loading CP/M and mocking the disk operations

### CP/M is taken from

https://github.com/Z80-Retro/cpm-2.2

[cpm.sys](https://github.com/Z80-Retro/cpm-2.2/blob/main/filesystem/cpm.sys) contains CP/M's core:
- BDOS (Basic Disk Operating System)
- CCP (Console Command Processor)

### BDOS Operations to support CP/M

| Func # | Function Name               | Description                                       |
| ------ | --------------------------- | ------------------------------------------------- |
| 0      | System Reset (Cold Boot)    | Reboot system (not normally used by programs).    |
| 1      | Console Input               | Wait for character from console.                  |
| 2      | Console Output              | Output character to console.                      |
| 3      | Reader Input                | Input from paper tape (legacy, unused).           |
| 4      | Punch Output                | Output to paper tape (legacy, unused).            |
| 5      | List Output                 | Send character to list device (usually printer).  |
| 6      | Direct Console I/O          | Low-level I/O: check status or input/output char. |
| 7      | Get I/O Byte                | Read the current I/O byte.                        |
| 8      | Set I/O Byte                | Set the current I/O byte.                         |
| 9      | Print String                | Print string at DE, terminated by `$`.            |
| 10     | Read Console Buffer         | DE points to buffer; reads line into it.          |
| 11     | Get Console Status          | A = 0xFF if character ready, else 0.              |
| 12     | Get Current Disk            | Returns drive number (A=0, B=1, etc.).            |
| 13     | Reset Disk System           | Re-initialize all disks.                          |
| 14     | Select Disk                 | C = drive (0=A), returns 0 if OK.                 |
| 15     | Open File                   | DE points to FCB.                                 |
| 16     | Close File                  | DE points to FCB.                                 |
| 17     | Search First                | DE points to FCB with filename mask.              |
| 18     | Search Next                 | Continue previous directory search.               |
| 19     | Delete File                 | DE points to FCB.                                 |
| 20     | Read Sequential             | DE points to FCB, reads next record.              |
| 21     | Write Sequential            | DE points to FCB, writes record.                  |
| 22     | Make File                   | DE points to FCB, creates file.                   |
| 23     | Rename File                 | DE points to FCB, use second FCB for new name.    |
| 24     | Return Login Vector         | HL = bitmap of logged-in drives.                  |
| 25     | Get Current DMA Address     | HL = DMA address.                                 |
| 26     | Set DMA Address             | DE = new DMA address.                             |
| 27     | Get ALV (Allocation Vector) | HL = pointer to allocation vector (internal).     |
| 28     | Get/Set User Number         | C=0x28, E=user (0–15), if E=FF: get only.         |
| 29     | Read Random Record          | DE = FCB, uses random record fields.              |
| 30     | Write Random Record         | Same as above.                                    |
| 31     | Compute File Size           | Updates FCB with total number of records.         |
| 32     | Set Random Record           | Computes and stores random record number in FCB.  |
| 33     | Reset Drive (optional)      | Used in CP/M 3+; not meaningful in 2.2.           |

### Testing

1. `CPMLoadTest` loads CP/M from `src/test/resources/cpm22/cpm.sys`
2. at a time of load the list of `.COM` files are pulled from `src/test/resources/cpm22/apps` and made available to CP/M as 'files on disk' (mocking through BDOS operations)
3. if you type `DIR`, CP/M must show the list of `.COM` files, e.g. `PLUS2.COM ...`
4. if you type `PLUS2` and tap `Enter`, the `PLUS2.COM` will be loaded into memory and executed

Result will be printed string to the console, e.g.
```
Running CP/M program... Press Ctrl+C to quit.

K>dir

K: PLUS2    COM
K>PLUS2
2 + 2 = 4

K>
```
