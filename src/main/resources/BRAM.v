module BRAM (clock, en, addr, dout);

parameter ADDR_WIDTH = 5;
parameter ADDR_DEPTH = 2**ADDR_WIDTH;
parameter DATA_WIDTH = 64;

input clock;
input en;
input [ADDR_WIDTH-1:0] addr;
output reg [DATA_WIDTH-1:0] dout;

(* rom_style = "block" *)
reg [DATA_WIDTH-1:0] rom [ADDR_DEPTH-1:0];


initial begin
	if (ADDR_DEPTH == 32)
		$readmemb("tw32.txt", rom);
	else if (ADDR_DEPTH == 64)
		$readmemb("tw64.txt", rom);
	else if (ADDR_DEPTH == 128)
		$readmemb("tw128.txt", rom);
	else if (ADDR_DEPTH == 256)
		$readmemb("tw256.txt", rom);
    else if (ADDR_DEPTH == 512)
        $readmemb("tw512.txt", rom);
    else if (ADDR_DEPTH == 1024)
        $readmemb("tw1024.txt", rom);
end

always @(posedge clock) begin
	if (en) begin
		dout <= rom[addr];
	end
end

endmodule : BRAM
