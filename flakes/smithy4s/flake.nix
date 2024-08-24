{
  description = "smithy4s cli";

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs"; };

  outputs = { self, nixpkgs }: let
    supportedSystems = [ "x86_64-linux" "x86_64-darwin" "aarch64-darwin" ];
    forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
  in {
    packages = forAllSystems (system: let
      pkgs = import nixpkgs { inherit system; };
      baseName = "smithy4s";
      version = "0.18.23";
      deps = pkgs.stdenv.mkDerivation {
        name = "${baseName}-deps-${version}";
        buildCommand = ''
          export COURSIER_CACHE=$(pwd)
          ${pkgs.coursier}/bin/cs fetch com.disneystreaming.smithy4s:smithy4s-codegen-cli_2.13:${version} > deps
          mkdir -p $out/share/java
          cp $(< deps) $out/share/java/
        '';
        outputHashMode = "recursive";
        outputHash = "sha256-pybvNYndZHcip5MFqNzACSsT8HJbL5uRD/uf9V65WhQ=";
      };
    in {
      smithy4s = pkgs.stdenv.mkDerivation {
        pname = baseName;
        inherit version;

        nativeBuildInputs = [ pkgs.makeWrapper pkgs.setJavaClassPath ];
        buildInputs = [ deps ];

        dontUnpack = true;

        installPhase = ''
          runHook preInstall

          mkdir -p $out/bin $out/share/java

          # Copy all JAR files to the share directory
          cp ${deps}/share/java/*.jar $out/share/java/

          makeWrapper ${pkgs.jre}/bin/java $out/bin/${baseName} \
            --add-flags "-cp $CLASSPATH smithy4s.codegen.cli.Main"

          runHook postInstall
        '';

        installCheckPhase = ''
          $out/bin/${baseName} --version | grep -q "${version}"
        '';

      };
    });
  };
}
