# click (test/python/wheels.edn): a group of commands with options, arguments, types and prompts, run by its CliRunner
import click
from click.testing import CliRunner


@click.group()
@click.option("--debug/--no-debug", default=False)
@click.pass_context
def cli(ctx, debug):
    """A tool."""
    ctx.obj = {"debug": debug}


@cli.command()
@click.argument("names", nargs=-1)
@click.option("-n", "--count", type=click.IntRange(1, 3), default=1, show_default=True)
@click.option("--shout", is_flag=True, help="Upper case.")
@click.pass_obj
def hello(obj, names, count, shout):
    """Greet NAMES."""
    for name in names or ("world",):
        for _ in range(count):
            msg = f"hello {name}" + (" (debug)" if obj["debug"] else "")
            click.echo(msg.upper() if shout else msg)


@cli.command()
@click.option("--kind", type=click.Choice(["a", "b"], case_sensitive=False), required=True)
@click.option("--size", type=float, envvar="SIZE")
@click.password_option()
def make(kind, size, password):
    click.echo(f"kind={kind} size={size} password={len(password)}")
    if size and size > 10:
        raise click.ClickException("too big")


runner = CliRunner()
for args, inp, env in [
    (["--help"], None, None),
    (["hello"], None, None),
    (["--debug", "hello", "a", "b", "-n", "2"], None, None),
    (["hello", "--shout", "x"], None, None),
    (["hello", "-n", "5"], None, None),
    (["hello", "--help"], None, None),
    (["make", "--kind", "B"], "pw\npw\n", None),
    (["make", "--kind", "c"], None, None),
    (["make", "--kind", "a", "--password", "p"], None, {"SIZE": "11.5"}),
    (["nope"], None, None),
]:
    r = runner.invoke(cli, args, input=inp, env=env)
    print(args, r.exit_code)
    print(r.output, end="")
print(click.style("x", fg="red", bold=True).encode(), click.unstyle(click.style("y", fg="blue")))
print(click.format_filename(b"a/b.txt"), click.get_app_dir("pyi", force_posix=True).endswith("/.pyi"))
