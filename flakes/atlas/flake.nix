{
  description = "Atlas Open Edition binary package";
  # https://atlasgo.io/community-edition#community-vs-other-editions

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs"; };

  outputs = { self, nixpkgs }: {
    packages.x86_64-linux =
      let pkgs = import nixpkgs { system = "x86_64-linux"; };
      in {
        atlas = pkgs.stdenv.mkDerivation {
          pname = "atlas";
          version = "latest";

          src = pkgs.fetchurl {
            url = "https://release.ariga.io/atlas/atlas-linux-amd64-v0.26.0";
            sha256 = "sha256-C/my+oOVX1DzdwuA2otud1Zer+0WOPjR749E8c03TOE=";
          };

          phases = [ "installPhase" ];

          installPhase = ''
            mkdir -p $out/bin
            install -m755 $src $out/bin/atlas
          '';
        };
      };
  };
}
