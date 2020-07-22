import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.core.CloneDetector
import io.github.t45k.clione.core.CloneTracker
import io.github.t45k.clione.core.Granularity
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.util.generatePRMock
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class ClioneGsonBlock {

    @Test
    fun test() {
        val resDir = if (Files.notExists(Path.of("result"))) {
            Files.createDirectory(Path.of("result"))
        } else {
            Path.of("result")
        }

        val dir = if (Files.notExists(resDir.resolve("gson_block"))) {
            Files.createDirectory(resDir.resolve("gson_block"))
        } else {
            resDir.resolve("gson_block")
        }

        """1512 fc63a6af4c0b34e808b4b373d9f24ef72df42a04 46d0f58d1266c17a9fe6354344e9b1b7145fcb3d
1300 4081dbaa6da4b5e14df5ac5eae23196b14827b8b bdea5b9e99012eacf1a7f35f5f23733c9caf8a89
1134 03a72e752ef68269990f984c9fd613cfd59224bc 68cf8fdc9576815e313ffbae2d9e2604dc2f16fa
1072 5412f21431ebace2295e933fa0446adc0abf4b01 3270e8d97259665cad9b66011993044de97c26db
1037 fd37cf1d0d3a37ade0e74f07e91c97a925ba42db d0e70bcdbf15aab8b03a05c88da4a4b4f60222dc
1016 2d072bae3add0fbdb74766bf9fad9fc54267a844 4644837207fba089f73ca0dd0b05a7750b960b38
1015 7719e73a108c450666613c279bedaed724cd9cc9 9c30b0e20389dfce7baf897685677a7ed42e42f3
1012 4a57ba6afddb75c2941100727d60e7e236d1e95a 9d8d7a43e1335005dbaea0f96c1f1c5d33a61531
982 4976e420fceaf0615f93dd3c59d5243b84160ffb 9e5e4ac630d6785b3a764a1c523a91605935d61d
922 2271525dd5d50311bd6b9bf8930ef8bb789928cd 688f918a353e57f11405fab17d82d506a4e7dc4c
900 8b464231f735b3157f38c6e589171dc17f709ba1 9414b9b3b61d59474a274aab21193391e5b97e52
881 f482f4a1cb47e53d0aca86e9d5035af6d2240a2d 193349f4aa7eecf43cc6ebf104e218e704db21d9
873 854760e6c7131b5ec5b1ec07756a3b2d0807aff3 1f859ec769ed3a220bf8adf2423ba29b44db94e2
872 ebad966efd8dfaf950b8e93fd39041e7fc5c4a27 c16be41e77bb53a4b639cb864c9a6e4d0f8df7c2
871 ecaa57114f3cc51e091d6699c0d82cd2c84a3a7f c2fae85a9f71d478d1153112e09dbc45ef31259a
870 03337640c2d98086ec0ab8e8ffba881d846f3f24 ecaa57114f3cc51e091d6699c0d82cd2c84a3a7f
857 0f80936ecdf4c4d32eaa044305efdb3fcdb81ffe 61f83d630935eec742f66346389fa3d2a13c0874
853 c101e31c6984c763ffdd786248fda871fdf6aaac c414b368e120a6240ceb238f4e0abce821a9f7cd
840 8537c8932f0d9bd3338a5048fb65a8506c7a8247 371aff7ce4fb73ed997cbf6aa08706f272cef0c6
839 d5c090835f18a6a4aa70010fa83249db359e5222 8537c8932f0d9bd3338a5048fb65a8506c7a8247
836 0f66f4fac441f7d7d7bc4afc907454f3fe4c0faa 59edfc1caf2bb30e30f523f8502f23e8f8edc38e
832 0f80936ecdf4c4d32eaa044305efdb3fcdb81ffe 3ff16c30dbcd2a66209935a3e71a5f21f696d896
830 a477f4f0ca9405ec85e65e1d9b8f2454e9ff1a0d 966de9e60dd6a828cb52548dd8de77adc86bd319
829 a477f4f0ca9405ec85e65e1d9b8f2454e9ff1a0d ab40462cc76f6708e3ce2e981f5c6b485347702b
826 48c430b811b8c8b6b3651527483f4bba2f13f659 9c4f3523206468746ef5ee135f2bced4e45b33b5
818 ee8d6be59ff6f2466d65be746b96ccf07ddb9ddf 34d7521d9581b025c23bd5e4880a1b3a687b3da0
809 a02f575797d9295bff8fb92d266b0f9724e42098 0669ff7fd16c26521b924a58c7dd64ab2dc1a46d
806 1f15d76b235b0dbd0880d5589414a6b3e17ca3df 31dcfa3ad6fff0ee64f0fc5b8a1712c3ef3dcf95
800 67bd3a2cf6f9c9c6e7f615969b1918f68e03932d 1ab73ffd21d8c08bbe734154921a936e4a8cb091
792 6f15cd0060dfed248eae0219d35f20d89022972c aa209fa2555271215a89fe382a4b9078102e6abd
773 2ab776b5f5075bd98e7eb730cb03772f2b734b45 c8627c8ab831dec86eeb52fc02600b22c81ba858
772 34f400582973ab41e39d942618398e83ff0efbec 23e2916947940864f828505531aa130fba6d8743
771 34f400582973ab41e39d942618398e83ff0efbec 3360c93a76a74d1182b912052973e08ffa868b43
770 1f803bd37de5890f6da53f0b1a1b631eea4d1b8f 61f58baaf926d3e6b16a52305537b3495f155ca8
769 1f803bd37de5890f6da53f0b1a1b631eea4d1b8f ed6298c98a35ccef795caa6cb39bcef01b64e274
761 e48c780389e62a8ace3a59f52935c8e79cef134c 7a1c94f9863bfc6915eaa56236dcef46876d4674
740 e48c780389e62a8ace3a59f52935c8e79cef134c d86dfdec7c18ceb002889d73f808a4be4ee7105f
738 f8105f0006fb81a15be1ff8eefe7a17488a29fe8 c935f89b23c69e7c9d6d98df87f81635b3eb3700
733 457f53f08f38e3c6d154699ea00fa521f208882d 10cefa49d2ade8f1ba99693509916128c8786a6d
731 64107353a37e623ed1f8fecb4422c24212cf6fe1 da4334b8dfcfa3efe6fba6b95afd0e5e6b272348
730 9e5f86d10b3b3ff4ba0dfe7ba0722c9e640fcc20 fef43b2aaa4fd7cb8f0dff395980c69f614635af
729 55b4a3f9b0adc2d494ae0b3bf747afb398f3b9dd 79e1c669ed8e02dcd645a701fc6afa7de12225db
719 93605e7145c989d4f38ae347c9f31d605efcf434 7d1973e6c5e270c0c94eaf6a3e81f4f2c5b2a699
709 095ebbd5a116599a044c85221bc24d01b685ad86 3b1671dde97a6d564b3f4c3210fbe777dc3aa3d4
707 8e1da9cf1ae18939eace17a147c386723d8fc267 2ecce944948e63f703e10cd34f4e2f410a7f3921
706 93605e7145c989d4f38ae347c9f31d605efcf434 1e9004403c619729ad1c49f070ab61427bd462c5
705 93605e7145c989d4f38ae347c9f31d605efcf434 96b2ada79aeaa44e4c572b345563d0ee90dde5b1
704 31f80d86590e523af1360a1a3d0f330989434286 e81f3eba2731bac5b7b5a50e7218f9847a170d83
700 2cac11b44984640ffa2ec5cccae0c8643b5bce92 ff2c8f8e8b118278947755898cb6c75dfc7e90ff
699 3361030766593c0b05e17dace401cce6fcdf9e24 109915d93a2b36c26b49c0fabb1947a5a892cb25
694 b9738875c049ed22a029b15180ed4ad117676f0a 618343fd1bae998ac9ebfde37a56eb4f498a59d7
689 b4978a8062220ae431750d93f76d737e83a5bd89 eb79ec73f01d31853e411843f64172ee1c5703d0
688 b4978a8062220ae431750d93f76d737e83a5bd89 64d74db8ae69f3dc37feb09df54e53ba0458edb7
685 09839be00420a6294628915defa93fdd042ff406 0c4ae018361d4756123c82c2f67ad04385caec5b
684 0a93efada5edd251516eb6927b295a1bd1864509 2ee680a64529ac6f31306e06d43eae8cf53f7c55
671 31ea72a29f5aaf2fc3d59eefbf72d5eb68c8b176 5cf82a573f78e587f44c58ccd9b84679d531bfd6
667 bb34247cc4283b91ed657238a37744eaa1b0f3e9 f7abd59a3b349d5c926507705f439afff54ed301
664 fcfd397d6fcbd4dd056625a0200ac6b2190520e6 82edd57205fe48015f81b2986f27856cb9f7fb29
657 2cac11b44984640ffa2ec5cccae0c8643b5bce92 032847976c0cba7c131e95aa6608882987f1eca8
652 2cac11b44984640ffa2ec5cccae0c8643b5bce92 6e57df7e9648f4a48b500ac9a181a058829bcee1""".trimMargin().split("\n")
            .forEach { line ->
                val (num, base, head) = line.split(" ")
                val repositoryFullName = "google/gson"
                val pullRequest = generatePRMock(repositoryFullName, num.toInt(), head, base)
                GitController.cloneIfNotExists(repositoryFullName, "", pullRequest).use { git ->
                    val config = RunningConfig(
                        "gson/src/main",
                        cloneDetector = CloneDetector.SOURCERERCC,
                        granularity = Granularity.BLOCK
                    )
                    val cloneTracker = CloneTracker(git, pullRequest, config)
                    val (old, new) = cloneTracker.track()
                    val result = "old\n\n" +
                        old.joinToString("\n\n") { cloneSet ->
                            cloneSet.joinToString("\n")
                        } +
                        "\n\nnew\n\n" +
                        new.joinToString("\n\n") { cloneSet ->
                            cloneSet.joinToString("\n")
                        }
                    Files.writeString(dir.resolve(Path.of(num)), result)
                }
            }
    }
}