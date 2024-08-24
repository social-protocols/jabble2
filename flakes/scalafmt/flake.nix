{
  description = "scalafmt native binary";
  # https://scalameta.org/scalafmt/docs/installation.html#native-image

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs"; };

  outputs = { self, nixpkgs }:
    let
      supportedSystems = [ "x86_64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
    in {
      packages = forAllSystems (system:
        let
          pkgs = import nixpkgs { inherit system; };
          version = builtins.readFile ./.scalafmt.conf;
          scalafmtVersion =
            builtins.head (builtins.match ''.*version = "([^"]*)".*'' version);

          urlForSystem = if pkgs.stdenv.isDarwin then
            "https://github.com/scalameta/scalafmt/releases/download/v${scalafmtVersion}/scalafmt-macos"
          else
            "https://github.com/scalameta/scalafmt/releases/download/v${scalafmtVersion}/scalafmt-linux-musl";

          sha256ForSystem = if pkgs.stdenv.isDarwin then
            "sha256-0rA1IPbiqEGxc6/vuW2ab3BnJskjZxviPCgUPyQKmLY="
          else
            "sha256-tZbltEBiFJz2303dgYAkCFH0f43EQSy9m4abwz3z7bs=";
        in {
          scalafmt = pkgs.stdenv.mkDerivation {
            pname = "scalafmt";
            version = scalafmtVersion;

            src = pkgs.fetchurl {
              url = urlForSystem;
              sha256 = sha256ForSystem;
            };

            phases = [ "installPhase" ];

            installPhase = ''
              mkdir -p $out/bin
              cp $src $out/bin/scalafmt
              chmod +x $out/bin/scalafmt
            '';

          };
        });
    };
}
