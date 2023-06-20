doteur
======

*An elegant dotfiles manager*

## What

doteur (a play on "auteur") aims to be a simple, intuitive tool for managing dotfiles. It supports managing multiple dotfiles "environments" at a time by recreating the minimal set of directories and symlinks in order to merge all the environments together in your home directory.

doteur takes inspiration for its UX from (and is largely compatible with) [dotfiler][dotfiler], but is designed to avoid unnecessary the home directory scanning that dotfiler performs, avoiding unnecessary slowdowns when unrelated files inevitably clutter your home directory.

## How

Install with [bbin][bbin]:

```bash
$ bbin install io.dhleong.github/doteur
```

`add` environments, then `update` to link them:

```bash
# Github repos are supported with the expected syntax,
# but you can also use any git URI
$ doteur add dhleong/dots
$ doteur update
```

If you can't figure out how to install `bbin` for whatever reason, [babashka][bb] should have instructions available. Then you can clone this repo and use `bb doteur` from its root directory, or use `bb package` to create a `doteur.jar` that `bb` can execute (`bb doteur.jar`).

[dotfiler]: https://github.com/svetlyak40wt/dotfiler
[bbin]: https://github.com/babashka/bbin#installation
[bb]: https://github.com/babashka/babashka#installation
