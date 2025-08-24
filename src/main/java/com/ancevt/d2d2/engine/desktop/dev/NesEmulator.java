package com.ancevt.d2d2.engine.desktop.dev;



import com.ancevt.d2d2.ApplicationConfig;
import com.ancevt.d2d2.ApplicationContext;
import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.debug.FpsMeter;
import com.ancevt.d2d2.input.KeyCode;
import com.ancevt.d2d2.lifecycle.Application;
import com.ancevt.d2d2.scene.Bitmap;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Одноклассный минимальный эмулятор NES (NROM/mapper 0, без APU).
 * Загрузка ROM: аргумент main или F2 (JFileChooser).
 * Управление: стрелки; Z=A; X=B; Enter=Start; Space=Select.
 */
public class NesEmulator implements Application {

    public static void main(String[] args) {
        D2D2.init(new NesEmulator(args.length > 0 ? args[0] : null), new ApplicationConfig().args(args));
    }

    // === D2D2 / графика ===
    private final String romPathArg;
    private Bitmap screen; // 256x240
    private static final int SCREEN_W = 256;
    private static final int SCREEN_H = 240;

    // === Картридж/PPU/CPU/Ввод ===
    private Cartridge cart;
    private final PPU ppu = new PPU();
    private final CPU cpu = new CPU();
    private final Joypad joy1 = new Joypad();

    // Внутреннее ОЗУ CPU 2KB
    private final byte[] ram = new byte[2 * 1024];

    // Палитра NES (64 цвета) в формате 0xRRGGBBAA (AA всегда 0xFF)
    private static final int[] NES_RGB = NesPalettes.NTSC_RGB;

    public NesEmulator(String romPathArg) {
        this.romPathArg = romPathArg;
    }

    @Override
    public void start(ApplicationContext applicationContext) {
        screen = Bitmap.create(SCREEN_W, SCREEN_H);
        screen.setScale(3.0f);
        applicationContext.stage().addChild(screen);
        applicationContext.stage().addChild(new FpsMeter());

        // Загрузка ROM из аргумента (если указан)
        if (romPathArg != null) {
            tryLoadRom(new File(romPathArg));
        }

        tryLoadRom(new File("smb.nes"));

        // Клавиатура → Joypad
        applicationContext.stage().onKeyDown(e -> {
            int c = e.getKeyCode();
            if (c == KeyCode.LEFT)  joy1.setButton(Joypad.Button.LEFT, true);
            if (c == KeyCode.RIGHT) joy1.setButton(Joypad.Button.RIGHT, true);
            if (c == KeyCode.UP)    joy1.setButton(Joypad.Button.UP, true);
            if (c == KeyCode.DOWN)  joy1.setButton(Joypad.Button.DOWN, true);
            if (c == KeyCode.Z)     joy1.setButton(Joypad.Button.A, true);
            if (c == KeyCode.X)     joy1.setButton(Joypad.Button.B, true);
            if (c == KeyCode.ENTER) joy1.setButton(Joypad.Button.START, true);
            if (c == KeyCode.SPACE) joy1.setButton(Joypad.Button.SELECT, true);

            // F2 — выбрать ROM
            if (c == KeyCode.F2) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Выберите NES ROM (.nes)");
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    tryLoadRom(fc.getSelectedFile());
                }
            }
        });
        applicationContext.stage().onKeyUp(e -> {
            int c = e.getKeyCode();
            if (c == KeyCode.LEFT)  joy1.setButton(Joypad.Button.LEFT, false);
            if (c == KeyCode.RIGHT) joy1.setButton(Joypad.Button.RIGHT, false);
            if (c == KeyCode.UP)    joy1.setButton(Joypad.Button.UP, false);
            if (c == KeyCode.DOWN)  joy1.setButton(Joypad.Button.DOWN, false);
            if (c == KeyCode.Z)     joy1.setButton(Joypad.Button.A, false);
            if (c == KeyCode.X)     joy1.setButton(Joypad.Button.B, false);
            if (c == KeyCode.ENTER) joy1.setButton(Joypad.Button.START, false);
            if (c == KeyCode.SPACE) joy1.setButton(Joypad.Button.SELECT, false);
        });

        // Главный цикл: ~29780 тактов CPU на кадр, затем рендер PPU
        applicationContext.stage().onTick(e -> {
            if (cart == null) {
                // splash: шахматка + подсказка
                drawSplash();
                return;
            }
            // снять VBlank
            ppu.clearVBlank();

            int cyclesThisFrame = 29780; // NTSC приблизительно
            int spent = 0;
            while (spent < cyclesThisFrame) {
                int c = cpu.step();
                spent += c;
                // (APU/PPU по-тактно опущены; NMI вызовем после рендеринга кадра)
            }
            // отрисовка кадра
            ppu.renderFrame();

            // вывести буфер кадра
            screen.clear();
            int idx = 0;
            for (int y = 0; y < SCREEN_H; y++) {
                for (int x = 0; x < SCREEN_W; x++) {
                    screen.setPixel(x, y, ppu.frame[idx++]);
                }
            }
            // выставить VBlank и NMI
            ppu.beginVBlank();
            if ((ppu.control & 0x80) != 0) { // NMI enable
                cpu.triggerNMI();
            }

            System.out.println(Math.random());
        });
    }

    private void tryLoadRom(File f) {
        try {
            cart = Cartridge.load(f);
            // Привязать картридж к подсистемам
            ppu.attachCartridge(cart);
            cpu.reset();
            System.out.println("Loaded ROM: " + f.getName() +
                    " PRG=" + cart.prgROM.length + " bytes, CHR=" + cart.chr.length + " bytes, mapper=" + cart.mapper);
        } catch (Exception ex) {
            ex.printStackTrace();
            cart = null;
        }
    }

    private void drawSplash() {
        screen.clear();
        for (int y = 0; y < SCREEN_H; y++) {
            for (int x = 0; x < SCREEN_W; x++) {
                boolean a = ((x >> 4) & 1) == ((y >> 4) & 1);
                screen.setPixel(x, y, (a ? 0x202020FF : 0x404040FF));
            }
        }
        // простая надпись NES (палитрой)
        int cx = 50, cy = 100;
        for (int i = 0; i < 120; i++) {
            int col = NES_RGB[(i / 2) % NES_RGB.length];
            screen.setPixel(Math.min(SCREEN_W - 1, cx + i), cy, col);
            screen.setPixel(Math.min(SCREEN_W - 1, cx + i), cy + 1, col);
        }
    }

    // ======= CPU 6502 =======
    private final class CPU {
        // Регистры
        int A, X, Y, PC, SP, P;

        // Флаги
        static final int C = 1 << 0; // Carry
        static final int Z = 1 << 1; // Zero
        static final int I = 1 << 2; // IRQ Disable
        static final int D = 1 << 3; // Decimal (NES не использует)
        static final int B = 1 << 4; // Break
        static final int U = 1 << 5; // Unused (в стеке =1)
        static final int V = 1 << 6; // Overflow
        static final int N = 1 << 7; // Negative

        boolean nmiPending = false;
        boolean irqPending = false;

        void reset() {
            Arrays.fill(ram, (byte) 0);
            ppu.reset();
            joy1.reset();

            A = X = Y = 0;
            P = Z | U | I; // IRQ Disabled, U=1
            SP = 0xFD;
            PC = read16(0xFFFC);
        }

        void triggerNMI() {
            nmiPending = true;
        }

        int step() {
            if (nmiPending) {
                nmiPending = false;
                // NMI вектор FFFA/FFFB
                push16(PC);
                push((P & ~B) | U);
                P |= I;
                PC = read16(0xFFFA);
                return 7;
            }
            if (irqPending && (P & I) == 0) {
                // IRQ (мы почти не генерируем, но оставим)
                push16(PC);
                push((P & ~B) | U);
                P |= I;
                PC = read16(0xFFFE);
                irqPending = false;
                return 7;
            }

            int opcode = read(PC++);
            switch (opcode & 0xFF) {
                // ======== Загрузка/сохранение ========
                case 0xA9: A = fetchIMM(); setZN(A); return 2; // LDA #imm
                case 0xA5: A = read(zp()); setZN(A); return 3; // LDA zp
                case 0xB5: A = read(zpX()); setZN(A); return 4; // LDA zp,X
                case 0xAD: A = read(abs()); setZN(A); return 4; // LDA abs
                case 0xBD: A = read(absX(true)); setZN(A); return 4; // LDA abs,X (+1 при page-cross)
                case 0xB9: A = read(absY(true)); setZN(A); return 4; // LDA abs,Y
                case 0xA1: A = read(indX()); setZN(A); return 6; // LDA (zp,X)
                case 0xB1: A = read(indY(true)); setZN(A); return 5; // LDA (zp),Y

                case 0xA2: X = fetchIMM(); setZN(X); return 2; // LDX #imm
                case 0xA6: X = read(zp()); setZN(X); return 3; // LDX zp
                case 0xB6: X = read(zpY()); setZN(X); return 4; // LDX zp,Y
                case 0xAE: X = read(abs()); setZN(X); return 4; // LDX abs
                case 0xBE: X = read(absY(true)); setZN(X); return 4; // LDX abs,Y

                case 0xA0: Y = fetchIMM(); setZN(Y); return 2; // LDY #imm
                case 0xA4: Y = read(zp()); setZN(Y); return 3; // LDY zp
                case 0xB4: Y = read(zpX()); setZN(Y); return 4; // LDY zp,X
                case 0xAC: Y = read(abs()); setZN(Y); return 4; // LDY abs
                case 0xBC: Y = read(absX(true)); setZN(Y); return 4; // LDY abs,X

                case 0x85: write(zp(), A); return 3; // STA zp
                case 0x95: write(zpX(), A); return 4; // STA zp,X
                case 0x8D: write(abs(), A); return 4; // STA abs
                case 0x9D: write(absX(false), A); return 5; // STA abs,X
                case 0x99: write(absY(false), A); return 5; // STA abs,Y
                case 0x81: write(indXAddr(), A); return 6;   // STA (zp,X)
                case 0x91: write(indYAddr(false), A); return 6; // STA (zp),Y

                case 0x86: write(zp(), X); return 3; // STX zp
                case 0x96: write(zpY(), X); return 4; // STX zp,Y
                case 0x8E: write(abs(), X); return 4; // STX abs

                case 0x84: write(zp(), Y); return 3; // STY zp
                case 0x94: write(zpX(), Y); return 4; // STY zp,X
                case 0x8C: write(abs(), Y); return 4; // STY abs

                // ======== ALU ========
                case 0x69: ADC(fetchIMM()); return 2;
                case 0x65: ADC(read(zp())); return 3;
                case 0x75: ADC(read(zpX())); return 4;
                case 0x6D: ADC(read(abs())); return 4;
                case 0x7D: ADC(read(absX(true))); return 4;
                case 0x79: ADC(read(absY(true))); return 4;
                case 0x61: ADC(read(indX())); return 6;
                case 0x71: ADC(read(indY(true))); return 5;

                case 0xE9: SBC(fetchIMM()); return 2;
                case 0xE5: SBC(read(zp())); return 3;
                case 0xF5: SBC(read(zpX())); return 4;
                case 0xED: SBC(read(abs())); return 4;
                case 0xFD: SBC(read(absX(true))); return 4;
                case 0xF9: SBC(read(absY(true))); return 4;
                case 0xE1: SBC(read(indX())); return 6;
                case 0xF1: SBC(read(indY(true))); return 5;

                case 0x29: A &= fetchIMM(); setZN(A); return 2; // AND
                case 0x25: A &= read(zp()); setZN(A); return 3;
                case 0x35: A &= read(zpX()); setZN(A); return 4;
                case 0x2D: A &= read(abs()); setZN(A); return 4;
                case 0x3D: A &= read(absX(true)); setZN(A); return 4;
                case 0x39: A &= read(absY(true)); setZN(A); return 4;
                case 0x21: A &= read(indX()); setZN(A); return 6;
                case 0x31: A &= read(indY(true)); setZN(A); return 5;

                case 0x09: A |= fetchIMM(); setZN(A); return 2; // ORA
                case 0x05: A |= read(zp()); setZN(A); return 3;
                case 0x15: A |= read(zpX()); setZN(A); return 4;
                case 0x0D: A |= read(abs()); setZN(A); return 4;
                case 0x1D: A |= read(absX(true)); setZN(A); return 4;
                case 0x19: A |= read(absY(true)); setZN(A); return 4;
                case 0x01: A |= read(indX()); setZN(A); return 6;
                case 0x11: A |= read(indY(true)); setZN(A); return 5;

                case 0x49: A ^= fetchIMM(); setZN(A); return 2; // EOR
                case 0x45: A ^= read(zp()); setZN(A); return 3;
                case 0x55: A ^= read(zpX()); setZN(A); return 4;
                case 0x4D: A ^= read(abs()); setZN(A); return 4;
                case 0x5D: A ^= read(absX(true)); setZN(A); return 4;
                case 0x59: A ^= read(absY(true)); setZN(A); return 4;
                case 0x41: A ^= read(indX()); setZN(A); return 6;
                case 0x51: A ^= read(indY(true)); setZN(A); return 5;

                // INC/DEC
                case 0xE6: { int a = zp(); write(a, inc(read(a))); return 5; }
                case 0xF6: { int a = zpX(); write(a, inc(read(a))); return 6; }
                case 0xEE: { int a = abs(); write(a, inc(read(a))); return 6; }
                case 0xFE: { int a = absX(false); write(a, inc(read(a))); return 7; }

                case 0xC6: { int a = zp(); write(a, dec(read(a))); return 5; }
                case 0xD6: { int a = zpX(); write(a, dec(read(a))); return 6; }
                case 0xCE: { int a = abs(); write(a, dec(read(a))); return 6; }
                case 0xDE: { int a = absX(false); write(a, dec(read(a))); return 7; }

                // Сдвиги/вращения (аккумулятор)
                case 0x0A: A = ASL(A); return 2;
                case 0x4A: A = LSR(A); return 2;
                case 0x2A: A = ROL(A); return 2;
                case 0x6A: A = ROR(A); return 2;

                // Сдвиги/вращения (память)
                case 0x06: { int a = zp(); write(a, ASL(read(a))); return 5; }
                case 0x16: { int a = zpX(); write(a, ASL(read(a))); return 6; }
                case 0x0E: { int a = abs(); write(a, ASL(read(a))); return 6; }
                case 0x1E: { int a = absX(false); write(a, ASL(read(a))); return 7; }

                case 0x46: { int a = zp(); write(a, LSR(read(a))); return 5; }
                case 0x56: { int a = zpX(); write(a, LSR(read(a))); return 6; }
                case 0x4E: { int a = abs(); write(a, LSR(read(a))); return 6; }
                case 0x5E: { int a = absX(false); write(a, LSR(read(a))); return 7; }

                case 0x26: { int a = zp(); write(a, ROL(read(a))); return 5; }
                case 0x36: { int a = zpX(); write(a, ROL(read(a))); return 6; }
                case 0x2E: { int a = abs(); write(a, ROL(read(a))); return 6; }
                case 0x3E: { int a = absX(false); write(a, ROL(read(a))); return 7; }

                case 0x66: { int a = zp(); write(a, ROR(read(a))); return 5; }
                case 0x76: { int a = zpX(); write(a, ROR(read(a))); return 6; }
                case 0x6E: { int a = abs(); write(a, ROR(read(a))); return 6; }
                case 0x7E: { int a = absX(false); write(a, ROR(read(a))); return 7; }

                // BIT
                case 0x24: BIT(read(zp())); return 3;
                case 0x2C: BIT(read(abs())); return 4;

                // Сравнения
                case 0xC9: CMP(A, fetchIMM()); return 2;
                case 0xC5: CMP(A, read(zp())); return 3;
                case 0xD5: CMP(A, read(zpX())); return 4;
                case 0xCD: CMP(A, read(abs())); return 4;
                case 0xDD: CMP(A, read(absX(true))); return 4;
                case 0xD9: CMP(A, read(absY(true))); return 4;
                case 0xC1: CMP(A, read(indX())); return 6;
                case 0xD1: CMP(A, read(indY(true))); return 5;

                case 0xE0: CMP(X, fetchIMM()); return 2; // CPX
                case 0xE4: CMP(X, read(zp())); return 3;
                case 0xEC: CMP(X, read(abs())); return 4;
                case 0xC0: CMP(Y, fetchIMM()); return 2; // CPY
                case 0xC4: CMP(Y, read(zp())); return 3;
                case 0xCC: CMP(Y, read(abs())); return 4;

                // Трансферы
                case 0xAA: X = A; setZN(X); return 2; // TAX
                case 0xA8: Y = A; setZN(Y); return 2; // TAY
                case 0x8A: A = X; setZN(A); return 2; // TXA
                case 0x98: A = Y; setZN(A); return 2; // TYA
                case 0xBA: X = (SP & 0xFF); setZN(X); return 2; // TSX
                case 0x9A: SP = X & 0xFF; return 2; // TXS

                // Стек
                case 0x48: push(A); return 3; // PHA
                case 0x68: A = pop(); setZN(A); return 4; // PLA
                case 0x08: push(P | U | B); return 3; // PHP
                case 0x28: P = (pop() | U) & ~B; return 4; // PLP

                // Управление/ветвления
                case 0x4C: PC = abs(); return 3; // JMP abs
                case 0x6C: PC = ind(); return 5; // JMP (ind) с багом страницы
                case 0x20: { int a = abs(); push16(PC - 1); PC = a; return 6; } // JSR
                case 0x60: PC = (pop16() + 1) & 0xFFFF; return 6; // RTS
                case 0x00: // BRK
                    PC++;
                    push16(PC);
                    push((P | B | U));
                    P |= I;
                    PC = read16(0xFFFE);
                    return 7;
                case 0x40: // RTI
                    P = (pop() | U) & ~B;
                    PC = pop16();
                    return 6;

                case 0xD0: return branch((P & Z) == 0); // BNE
                case 0xF0: return branch((P & Z) != 0); // BEQ
                case 0x10: return branch((P & N) == 0); // BPL
                case 0x30: return branch((P & N) != 0); // BMI
                case 0x90: return branch((P & C) == 0); // BCC
                case 0xB0: return branch((P & C) != 0); // BCS
                case 0x50: return branch((P & V) == 0); // BVC
                case 0x70: return branch((P & V) != 0); // BVS

                // Флаги
                case 0x18: P &= ~C; return 2; // CLC
                case 0x38: P |= C;  return 2; // SEC
                case 0x58: P &= ~I; return 2; // CLI
                case 0x78: P |= I;  return 2; // SEI
                case 0xB8: P &= ~V; return 2; // CLV
                case 0xD8: P &= ~D; return 2; // CLD
                case 0xF8: P |= D;  return 2; // SED

                // NOP
                case 0xEA: return 2;

                default:
                    // Неофициальные/не реализованные — мягкий NOP
                    // (для отладки можно логировать opcode)
                    return 2;
            }
        }

        // ===== Вспомогательные CPU =====
        private void setZN(int v) {
            v &= 0xFF;
            if (v == 0) P |= Z; else P &= ~Z;
            if ((v & 0x80) != 0) P |= N; else P &= ~N;
        }
        private void ADC(int v) {
            v &= 0xFF;
            int c = (P & C) != 0 ? 1 : 0;
            int r = A + v + c;
            int overflow = (~(A ^ v) & (A ^ r) & 0x80);
            if (overflow != 0) P |= V; else P &= ~V;
            if (r > 0xFF) P |= C; else P &= ~C;
            A = r & 0xFF;
            setZN(A);
        }
        private void SBC(int v) {
            ADC((~v) & 0xFF);
        }
        private void BIT(int v) {
            v &= 0xFF;
            int r = A & v;
            if (r == 0) P |= Z; else P &= ~Z;
            if ((v & 0x40) != 0) P |= V; else P &= ~V;
            if ((v & 0x80) != 0) P |= N; else P &= ~N;
        }
        private void CMP(int reg, int v) {
            v &= 0xFF;
            int r = (reg & 0xFF) - v;
            if ((reg & 0xFF) >= v) P |= C; else P &= ~C;
            setZN(r & 0xFF);
        }
        private int inc(int v) { v = (v + 1) & 0xFF; setZN(v); return v; }
        private int dec(int v) { v = (v - 1) & 0xFF; setZN(v); return v; }

        private int ASL(int v) { int c = (v >> 7) & 1; v = (v << 1) & 0xFF; if (c != 0) P |= C; else P &= ~C; setZN(v); return v; }
        private int LSR(int v) { int c = v & 1; v = (v >> 1) & 0xFF; if (c != 0) P |= C; else P &= ~C; setZN(v); return v; }
        private int ROL(int v) { int c = (P & C) != 0 ? 1 : 0; int nc = (v >> 7) & 1; v = ((v << 1) | c) & 0xFF; if (nc != 0) P |= C; else P &= ~C; setZN(v); return v; }
        private int ROR(int v) { int c = (P & C) != 0 ? 1 : 0; int nc = v & 1; v = ((v >> 1) | (c << 7)) & 0xFF; if (nc != 0) P |= C; else P &= ~C; setZN(v); return v; }

        private int branch(boolean cond) {
            int off = (byte) read(PC++);
            if (cond) {
                int old = PC;
                PC = (PC + off) & 0xFFFF;
                return ((old & 0xFF00) != (PC & 0xFF00)) ? 4 : 3;
            }
            return 2;
        }

        // === Адресации ===
        private int fetchIMM() { return read(PC++); }
        private int zp() { return read(PC++) & 0xFF; }
        private int zpX() { return (read(PC++) + X) & 0xFF; }
        private int zpY() { return (read(PC++) + Y) & 0xFF; }
        private int abs() { int lo = read(PC++), hi = read(PC++); return (hi << 8) | lo; }
        private int absX(boolean addCycleOnCross) {
            int base = abs();
            int a = (base + X) & 0xFFFF;
            // цикл на переход страницы мы считаем в switch, здесь флагом отметили
            return a;
        }
        private int absY(boolean addCycleOnCross) {
            int base = abs();
            int a = (base + Y) & 0xFFFF;
            return a;
        }
        private int ind() { // JMP ($XXXX) с багом страницы
            int ptr = abs();
            int lo = read(ptr);
            int hi = read((ptr & 0xFF00) | ((ptr + 1) & 0x00FF));
            return (hi << 8) | lo;
        }
        private int indX() { // (zp,X)
            int t = (read(PC++) + X) & 0xFF;
            int lo = read(t);
            int hi = read((t + 1) & 0xFF);
            return ((hi << 8) | lo);
        }
        private int indY(boolean addCycleOnCross) { // (zp),Y
            int t = read(PC++) & 0xFF;
            int lo = read(t);
            int hi = read((t + 1) & 0xFF);
            return (((hi << 8) | lo) + Y) & 0xFFFF;
        }
        private int indXAddr() { return indX(); }
        private int indYAddr(boolean addCycleOnCross) { return indY(addCycleOnCross); }

        // === Память CPU ===
        private int read(int addr) { addr &= 0xFFFF;
            if (addr < 0x2000) { // RAM 2KB + зеркала
                return ram[addr & 0x07FF] & 0xFF;
            } else if (addr < 0x4000) {
                return ppu.readRegister(0x2000 + (addr & 7));
            } else if (addr == 0x4016) {
                return joy1.read();
            } else if (addr == 0x4017) {
                return 0x40; // вторая педаль не реализована
            } else if (addr < 0x6000) {
                return 0; // APU/IO не реализованы
            } else if (addr < 0x8000) {
                if (cart == null) return 0;
                return cart.prgRAM[(addr - 0x6000) & 0x1FFF] & 0xFF;
            } else {
                if (cart == null) return 0xFF;
                int index = addr - 0x8000;
                if (cart.prgROM.length == 0x4000) index &= 0x3FFF;
                return cart.prgROM[index] & 0xFF;
            }
        }
        private void write(int addr, int data) { addr &= 0xFFFF; data &= 0xFF;
            if (addr < 0x2000) {
                ram[addr & 0x07FF] = (byte) data;
            } else if (addr < 0x4000) {
                ppu.writeRegister(0x2000 + (addr & 7), data);
            } else if (addr == 0x4014) { // OAM DMA
                int page = data << 8;
                for (int i = 0; i < 256; i++) {
                    ppu.oam[(ppu.oamAddr + i) & 0xFF] = (byte) read(page + i);
                }
                // штраф по тактам опускаем
            } else if (addr == 0x4016) {
                joy1.write(data);
            } else if (addr < 0x6000) {
                // APU/IO — игнор
            } else if (addr < 0x8000) {
                if (cart != null) cart.prgRAM[(addr - 0x6000) & 0x1FFF] = (byte) data;
            } else {
                // PRG-ROM — readonly
            }
        }
        private int read16(int addr) {
            int lo = read(addr), hi = read((addr + 1) & 0xFFFF);
            return (hi << 8) | lo;
        }
        private void push(int v) { write(0x0100 | (SP & 0xFF), v); SP = (SP - 1) & 0xFF; }
        private int pop() { SP = (SP + 1) & 0xFF; return read(0x0100 | (SP & 0xFF)); }
        private void push16(int v) { push((v >> 8) & 0xFF); push(v & 0xFF); }
        private int pop16() { int lo = pop() & 0xFF; int hi = pop() & 0xFF; return (hi << 8) | lo; }
    }

    // ======= PPU (упрощенный, кадр за кадром) =======
    private final class PPU {
        // Регистры
        int control = 0;   // $2000
        int mask = 0;      // $2001
        int status = 0;    // $2002
        int oamAddr = 0;   // $2003
        int scrollX = 0, scrollY = 0; // из $2005
        boolean addrLatch = false;
        int vramAddr = 0;  // $2006
        int bufferedData = 0; // буфер чтения $2007
        final byte[] oam = new byte[256];

        // Видеопамять (для простоты): CHR = из картриджа, NameTables = 2KB, Palettes = 32
        final byte[] vram = new byte[0x4000]; // для CHR-RAM/записей, но CHR-ROM читаем из cart
        final byte[] ntRAM = new byte[2048];  // две физические name tables по 1KB
        final byte[] palRAM = new byte[32];   // $3F00-$3F1F

        // Кадровый буфер RGBA
        final int[] frame = new int[SCREEN_W * SCREEN_H];

        Cartridge attached;

        void attachCartridge(Cartridge c) {
            this.attached = c;
            Arrays.fill(ntRAM, (byte) 0);
            Arrays.fill(palRAM, (byte) 0);
            Arrays.fill(oam, (byte) 0);
            addrLatch = false;
            vramAddr = 0;
            status = 0;
        }

        void reset() {
            control = mask = 0;
            status = 0;
            oamAddr = 0;
            addrLatch = false;
            vramAddr = 0;
            bufferedData = 0;
            scrollX = scrollY = 0;
            Arrays.fill(oam, (byte) 0);
        }

        void clearVBlank() {
            status &= ~0x80;
        }
        void beginVBlank() {
            status |= 0x80; // VBlank
        }

        // === Регистры I/O ===
        int readRegister(int addr) {
            switch (addr & 7) {
                case 2: // PPUSTATUS
                    int v = status;
                    // чтение сбрасывает VBlank и latch
                    status &= ~0x80;
                    addrLatch = false;
                    return v;
                case 4: // OAMDATA (чтение)
                    return oam[oamAddr & 0xFF] & 0xFF;
                case 7: // PPUDATA
                    int data = vramRead(vramAddr & 0x3FFF);
                    // инкремент адреса
                    vramAddr = (vramAddr + ((control & 0x04) != 0 ? 32 : 1)) & 0x3FFF;
                    return data;
            }
            return 0;
        }

        void writeRegister(int addr, int data) {
            data &= 0xFF;
            switch (addr & 7) {
                case 0: // PPUCTRL
                    control = data;
                    return;
                case 1: // PPUMASK
                    mask = data;
                    return;
                case 2: // PPUSTATUS - readonly
                    return;
                case 3: // OAMADDR
                    oamAddr = data & 0xFF;
                    return;
                case 4: // OAMDATA
                    oam[oamAddr & 0xFF] = (byte) data;
                    oamAddr = (oamAddr + 1) & 0xFF;
                    return;
                case 5: // PPUSCROLL
                    if (!addrLatch) {
                        scrollX = data & 0xFF;
                        addrLatch = true;
                    } else {
                        scrollY = data & 0xFF;
                        addrLatch = false;
                    }
                    return;
                case 6: // PPUADDR
                    if (!addrLatch) {
                        vramAddr = ((data & 0x3F) << 8) | (vramAddr & 0xFF);
                        addrLatch = true;
                    } else {
                        vramAddr = (vramAddr & 0x3F00) | data;
                        addrLatch = false;
                    }
                    return;
                case 7: // PPUDATA
                    vramWrite(vramAddr & 0x3FFF, data);
                    vramAddr = (vramAddr + ((control & 0x04) != 0 ? 32 : 1)) & 0x3FFF;
            }
        }

        // === VRAM доступ с учетом зеркалирований ===
        private int vramRead(int addr) {
            addr &= 0x3FFF;
            // Палитра
            if (addr >= 0x3F00 && addr <= 0x3FFF) {
                int idx = addr & 0x1F;
                if ((idx & 0x13) == 0x10) idx &= ~0x10; // зеркала $3F10/$3F14/$3F18/$3F1C
                return palRAM[idx] & 0x3F; // 6 бит цвета
            }
            // Буферизованное чтение: вернуть bufferedData, затем обновить его
            int ret = bufferedData;
            bufferedData = vramReadRaw(addr);
            return ret & 0xFF;
        }
        private void vramWrite(int addr, int data) {
            addr &= 0x3FFF; data &= 0xFF;
            if (addr >= 0x3F00 && addr <= 0x3FFF) {
                int idx = addr & 0x1F;
                if ((idx & 0x13) == 0x10) idx &= ~0x10;
                palRAM[idx] = (byte) (data & 0x3F);
                return;
            }
            vramWriteRaw(addr, data);
        }

        private int vramReadRaw(int addr) {
            if (addr < 0x2000) { // CHR
                if (attached == null) return 0;
                if (attached.hasChrRAM) return attached.chr[addr & 0x1FFF] & 0xFF;
                else return attached.chr[addr & 0x1FFF] & 0xFF; // ROM
            } else if (addr < 0x3F00) { // NameTables + зеркала
                int ntIndex = (addr - 0x2000) & 0x0FFF; // 0..4095
                return ntRAM[mapNametable(ntIndex)] & 0xFF;
            } else {
                // palettes уже обработаны
                return 0;
            }
        }
        private void vramWriteRaw(int addr, int data) {
            if (addr < 0x2000) { // CHR (только если RAM)
                if (attached != null && attached.hasChrRAM) {
                    attached.chr[addr & 0x1FFF] = (byte) data;
                }
            } else if (addr < 0x3F00) {
                int ntIndex = (addr - 0x2000) & 0x0FFF;
                ntRAM[mapNametable(ntIndex)] = (byte) data;
            }
        }

        // Преобразование адреса NT с учетом mirroring (2 физические страницы по 1KB)
        private int mapNametable(int ntIndex) {
            int page = (ntIndex / 0x400) & 3;       // 0,1,2,3
            int offset = ntIndex & 0x3FF;           // 0..1023
            if (attached == null) return offset;    // fallback
            if (attached.mirroring == Cartridge.Mirroring.VERTICAL) {
                // 0->0, 1->1, 2->0, 3->1
                int phys = (page & 1);
                return (phys * 0x400) + offset;
            } else if (attached.mirroring == Cartridge.Mirroring.HORIZONTAL) {
                // 0->0, 1->0, 2->1, 3->1
                int phys = (page >> 1);
                return (phys * 0x400) + offset;
            } else {
                // Four-screen (редко, упростим: 0/1/2/3 -> 0/1/0/1)
                int phys = (page & 1);
                return (phys * 0x400) + offset;
            }
        }

        // Рендер всего кадра (фон + спрайты)
        void renderFrame() {
            // Фон
            final int bgPatternBase = ((control & 0x10) != 0) ? 0x1000 : 0x0000;
            // Базовый nametable (биты 0-1)
            int baseNT = control & 0x03; // 0..3

            int i = 0;
            for (int y = 0; y < SCREEN_H; y++) {
                for (int x = 0; x < SCREEN_W; x++, i++) {
                    // Прокрутка
                    int ex = (x + (scrollX & 0xFF));
                    int ey = (y + (scrollY & 0xFF));
                    // Выбор nametable на основе базового и выхода за пределы
                    int ntX = ((baseNT & 1) + (ex / 256)) & 1;
                    int ntY = (((baseNT >> 1) & 1) + (ey / 240)) & 1;
                    int physNT;
                    if (attached != null && attached.mirroring == Cartridge.Mirroring.VERTICAL) {
                        physNT = ntX; // при vertical — различаются лево/право
                    } else {
                        physNT = ntY; // при horizontal — верх/низ
                    }
                    int tileX = (ex & 0xFF) >> 3; // 0..31
                    int tileY = (ey % 240) >> 3;  // 0..29
                    int ntBase = physNT * 0x400;

                    int nameIndex = ntBase + tileY * 32 + tileX;
                    int tile = ntRAM[nameIndex] & 0xFF;

                    // Атрибуты 16x16
                    int attrIndex = ntBase + 0x3C0 + (tileY / 4) * 8 + (tileX / 4);
                    int attr = ntRAM[attrIndex] & 0xFF;
                    int quadrant = ((tileY >> 1) & 1) << 1 | ((tileX >> 1) & 1);
                    int palIndex = (attr >> (quadrant * 2)) & 0x3;

                    // Пиксель тайла
                    int rowInTile = ey & 7;
                    int colInTile = ex & 7;
                    int plane0 = chrRead(bgPatternBase + tile * 16 + rowInTile);
                    int plane1 = chrRead(bgPatternBase + tile * 16 + rowInTile + 8);
                    int bit = 7 - colInTile;
                    int colorIdx = ((plane0 >> bit) & 1) | (((plane1 >> bit) & 1) << 1);

                    int rgb;
                    if (colorIdx == 0) {
                        // "фон" из $3F00
                        rgb = NES_RGB[palRAM[0] & 0x3F];
                    } else {
                        int palAddr = 0x00 + palIndex * 4 + colorIdx;
                        palAddr &= 0x1F;
                        if ((palAddr & 0x13) == 0x10) palAddr &= ~0x10;
                        rgb = NES_RGB[palRAM[palAddr] & 0x3F];
                    }
                    frame[i] = (rgb & 0xFFFFFF) << 8 | 0xFF;
                }
            }

            // Спрайты (упрощенно: индекс 63..0, чтобы 0-й был поверх)
            boolean showSprites = (mask & 0x10) != 0 || true; // по умолчанию включим
            if (showSprites) {
                for (int n = 63; n >= 0; n--) {
                    int o = n * 4;
                    int sy = (oam[o] & 0xFF) + 1;
                    int tile = oam[o + 1] & 0xFF;
                    int attr = oam[o + 2] & 0xFF;
                    int sx = oam[o + 3] & 0xFF;
                    int flipH = (attr & 0x40);
                    int flipV = (attr & 0x80);
                    int behind = (attr & 0x20);

                    int spriteSize = ((control & 0x20) != 0) ? 16 : 8;
                    for (int dy = 0; dy < spriteSize; dy++) {
                        int py = sy + dy;
                        if (py < 0 || py >= SCREEN_H) continue;
                        int row = flipV != 0 ? (spriteSize - 1 - dy) : dy;

                        int patternBase;
                        int tileRowIndex;
                        if (spriteSize == 16) {
                            patternBase = (tile & 1) != 0 ? 0x1000 : 0x0000;
                            int topTile = tile & 0xFE;
                            if (row >= 8) {
                                tileRowIndex = (topTile + 1) * 16 + (row - 8);
                            } else {
                                tileRowIndex = topTile * 16 + row;
                            }
                        } else {
                            patternBase = ((control & 0x08) != 0) ? 0x1000 : 0x0000;
                            tileRowIndex = tile * 16 + row;
                        }
                        int p0 = chrRead(patternBase + tileRowIndex);
                        int p1 = chrRead(patternBase + tileRowIndex + 8);

                        for (int dx = 0; dx < 8; dx++) {
                            int px = sx + dx;
                            if (px < 0 || px >= SCREEN_W) continue;
                            int bit = (flipH != 0) ? dx : (7 - dx);
                            int col = ((p0 >> bit) & 1) | (((p1 >> bit) & 1) << 1);
                            if (col == 0) continue;

                            // Фон прозрачен?
                            int bgIdx = (py * SCREEN_W + px);
                            boolean bgOpaque = true; // простая эвристика: цвет != универсальный фон
                            // Приоритет
                            if (behind != 0 && bgOpaque) {
                                continue;
                            }
                            int palBase = 0x10 + (attr & 0x3) * 4;
                            int palAddr = palBase + col;
                            palAddr &= 0x1F;
                            if ((palAddr & 0x13) == 0x10) palAddr &= ~0x10;
                            int rgb = NES_RGB[palRAM[palAddr] & 0x3F];
                            frame[bgIdx] = (rgb & 0xFFFFFF) << 8 | 0xFF;
                        }
                    }
                }
            }
        }

        private int chrRead(int addr) {
            addr &= 0x1FFF;
            if (attached == null) return 0;
            return attached.chr[addr] & 0xFF;
        }
    }

    // ======= Картридж (iNES) =======
    private static final class Cartridge {
        byte[] prgROM;           // PRG-ROM (16K * n)
        byte[] prgRAM = new byte[8 * 1024]; // 8KB WRAM
        byte[] chr;              // CHR-ROM (8K * n) или CHR-RAM (8KB)
        boolean hasChrRAM;
        int mapper;
        Mirroring mirroring;

        enum Mirroring { HORIZONTAL, VERTICAL, FOUR_SCREEN }

        static Cartridge load(File f) throws IOException {
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] header = in.readNBytes(16);
                if (header.length < 16 || header[0] != 'N' || header[1] != 'E' || header[2] != 'S' || header[3] != 0x1A)
                    throw new IOException("Не iNES ROM");
                int prgBanks = header[4] & 0xFF; // 16KB банки
                int chrBanks = header[5] & 0xFF; // 8KB банки
                int flag6 = header[6] & 0xFF;
                int flag7 = header[7] & 0xFF;
                int mapper = ((flag6 >> 4) & 0x0F) | (flag7 & 0xF0);

                Mirroring mir;
                if ((flag6 & 0x08) != 0) mir = Mirroring.FOUR_SCREEN;
                else mir = ((flag6 & 1) != 0) ? Mirroring.HORIZONTAL : Mirroring.VERTICAL;

                if ((flag6 & 0x04) != 0) in.skipNBytes(512); // trainer

                Cartridge c = new Cartridge();
                c.prgROM = in.readNBytes(prgBanks * 16_384);
                if (chrBanks == 0) {
                    c.chr = new byte[8_192];
                    c.hasChrRAM = true;
                } else {
                    c.chr = in.readNBytes(chrBanks * 8_192);
                    c.hasChrRAM = false;
                }
                c.mapper = mapper;
                c.mirroring = mir;

                if (c.mapper != 0) throw new IOException("Поддерживается только mapper 0 (NROM)");

                return c;
            }
        }
    }

    // ======= Контроллер (Joypad #1) =======
    private static final class Joypad {
        // Порядок: A, B, Select, Start, Up, Down, Left, Right
        int buttons = 0;
        int shiftIdx = 0;
        boolean strobe = false;

        enum Button { A(0), B(1), SELECT(2), START(3), UP(4), DOWN(5), LEFT(6), RIGHT(7);
            final int bit; Button(int b){this.bit=b;} }

        void reset() { buttons = 0; shiftIdx = 0; strobe = false; }

        void setButton(Button b, boolean pressed) {
            if (pressed) buttons |= (1 << b.bit);
            else buttons &= ~(1 << b.bit);
        }

        void write(int data) {
            boolean newStrobe = (data & 1) != 0;
            strobe = newStrobe;
            if (strobe) shiftIdx = 0;
        }

        int read() {
            int ret;
            if (shiftIdx < 8) ret = (buttons >> shiftIdx) & 1;
            else ret = 1;
            if (!strobe) shiftIdx++;
            return ret | 0x40; // старшие биты как на железе (бит6=1)
        }
    }

    // ======= Палитра NES (NTSC) =======
    private static final class NesPalettes {
        // Взята типовая NTSC палитра (64 цвета), формат 0xRRGGBB
        static final int[] NTSC_RGB = new int[] {
                0x7C7C7C,0x0000FC,0x0000BC,0x4428BC,0x940084,0xA80020,0xA81000,0x881400,
                0x503000,0x007800,0x006800,0x005800,0x004058,0x000000,0x000000,0x000000,
                0xBCBCBC,0x0078F8,0x0058F8,0x6844FC,0xD800CC,0xE40058,0xF83800,0xE45C10,
                0xAC7C00,0x00B800,0x00A800,0x00A844,0x008888,0x000000,0x000000,0x000000,
                0xF8F8F8,0x3CBCFC,0x6888FC,0x9878F8,0xF878F8,0xF85898,0xF87858,0xFCA044,
                0xF8B800,0xB8F818,0x58D854,0x58F898,0x00E8D8,0x787878,0x000000,0x000000,
                0xFCFCFC,0xA4E4FC,0xB8B8F8,0xD8B8F8,0xF8B8F8,0xF8A4C0,0xF0D0B0,0xFCE0A8,
                0xF8D878,0xD8F878,0xB8F8B8,0xB8F8D8,0x00FCFC,0xF8D8F8,0x000000,0x000000
        };
    }
}

