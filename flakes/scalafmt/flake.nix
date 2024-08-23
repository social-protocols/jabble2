{
  description = "scalafmt native binary";
  # https://scalameta.org/scalafmt/docs/installation.html#native-image

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs"; };

  outputs = { self, nixpkgs }: {
    packages.x86_64-linux = let
      pkgs = import nixpkgs { system = "x86_64-linux"; };
      version = builtins.readFile ./.scalafmt.conf;
      scalafmtVersion = builtins.match ''.*version = "([^"]*)".*'' version;
    in {
      scalafmt = pkgs.stdenv.mkDerivation {
        pname = "scalafmt";
        version = builtins.head scalafmtVersion;

        src = pkgs.fetchurl {
          url = "https://github.com/scalameta/scalafmt/releases/download/v${
              builtins.head scalafmtVersion
            }/scalafmt-linux-musl";
          sha256 = "sha256-tZbltEBiFJz2303dgYAkCFH0f43EQSy9m4abwz3z7bs=";
        };

        phases = [ "installPhase" ];

        installPhase = ''
          mkdir -p $out/bin
          cp $src $out/bin/scalafmt
          chmod +x $out/bin/scalafmt
        '';
      };
    };
  };
}
